package dev.chaosrig.utils.ping;

import dev.chaosrig.ChaosRigApi;
import dev.chaosrig.ChaosRigApiClient;
import dev.chaosrig.utils.ColorTools;
import dev.chaosrig.utils.KeyboardInput;
import dev.chaosrig.utils.PacketList;
import dev.chaosrig.utils.Vec3dHelper;
import dev.chaosrig.utils.config.ClientChaosRigApiConfig;
import dev.chaosrig.utils.config.ServerChaosRigApiConfig;
import dev.chaosrig.utils.data.DataConsumer;
import dev.chaosrig.utils.data.EntityAccessData;
import dev.chaosrig.utils.renderer.InformationScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>标记渲染器</p>
 * <p>
 *     该渲染器支持服务端存在与不存在形式 <br>
 *     该渲染器在本地客户端会进行独立<code>tick</code>管理元素, 也就是说可能遇到服务端未结束的元素, 客户端已结束的情况 <br>
 *     若服务端存在, 则会接收服务端数据并同步信息(元素不直接替换), 往往在服务端添加/删除/取消元素会进行同步 <br>
 *     若服务端不存在, 则会主动在{@link PingRenderer#pings}进行管理元素
 * </p>
 * <p>
 *     该类会进行渲染{@link ClientPingRecord#render(WorldRenderContext)}方法
 * </p>
 * @see ClientPingRecord
 * @see PingManager
 */
@Environment(EnvType.CLIENT)
public class PingRenderer extends DataConsumer {
    protected final Queue<ClientPingRecord> pings = new ConcurrentLinkedQueue<>();
    protected static final PingRenderer INSTANCE = new PingRenderer();

    public PingRenderer() {
        super(true);
        InformationScreen.push("[client] pings", -1, ColorTools.WHITE.apply(255), this.pings::toString); //test only
    }

    public static PingRenderer getInstance() {
        return INSTANCE;
    }

    public static void register() {
        if (ChaosRigApiClient.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        ClientPlayNetworking.registerGlobalReceiver(PacketList.SERVER_SYNC_PING_DATA, PingRenderer::syncFromServer);
        WorldRenderEvents.LAST.register(PingRenderer::render);
    }

    protected static void syncFromServer(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender packetSender) {
        ClientWorld world = client.world;
        if (world == null) return;
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (buf.readBoolean()) {
            INSTANCE.pings.clear();
            return;
        }
        Set<ClientPingRecord> tmpPings = buf.readCollection(HashSet::new, packetBuf -> {
            UUID uuid = packetBuf.readUuid();
            LivingEntity sender = EntityAccessData.findEntity(world, uuid);
            // get sender instance
            if (sender == null) {
                return null;
            }
            // prepare to create Object
            PingRecord.Type type = packetBuf.readEnumSet(PingRecord.Type.class).stream().findFirst().orElseThrow();
            int tick = packetBuf.readInt();
            int maxTick = packetBuf.readInt();
            return switch (type) {
                case ENTITY -> {
                    UUID target = packetBuf.readUuid();
                    if (sender.getUuid().equals(target)) {
                        yield null;
                    }
                    for (Entity entity : world.getEntities()) {
                        if (entity.getUuid().equals(target)) {
                            ClientPingRecord ping = new ClientEntityPingRecord(player, new EntityHitResult(entity), maxTick);
                            ping.tick = tick;
                            yield ping;
                        }
                    }
                    yield null;
                }
                case BLOCK -> {
                    ClientPingRecord ping = new ClientBlockPingRecord(player, packetBuf.readBlockHitResult(), maxTick);
                    ping.tick = tick;
                    yield ping;
                }
                case REGROUP -> {
                    ClientPingRecord ping = new ClientRegroupPingRecord(player, maxTick);
                    ping.tick = tick;
                    yield ping;
                }
                default -> {
                    ClientPingRecord ping = new ClientPlacePingRecord(player, packetBuf.readBlockHitResult(), maxTick);
                    ping.tick = tick;
                    yield ping;
                }
            };
        });
        INSTANCE.pings.removeIf(p -> !tmpPings.contains(p));
        // update
        for (ClientPingRecord tmpPing : tmpPings) {
            if (tmpPing == null) {
                continue;
            }
            if (!INSTANCE.pings.contains(tmpPing)) {
                INSTANCE.pings.add(tmpPing);
            } else {
                INSTANCE.pings.stream().filter(p -> p.equals(tmpPing)).forEach(p -> {
                    ClientPingRecord.copy(tmpPing, p);
                });
            }
        }
    }

    /**
     * <p>向服务端发送数据包：添加标记元素</p>
     * <p>该方法只有编写了{@link EntityHitResult}和{@link BlockHitResult}的判断, 即根据{@link PingRecord.Type}进行针对性编写</p>
     * @param ping 目标标记元素
     */
    public static void sendPingPacket(@NotNull ClientPingRecord ping) {
        if (!ChaosRigApiClient.isServerExist()) {
            throw new RuntimeException("服务端不存在, 无法通过发送数据包添加Ping");
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumSet(EnumSet.of(ping.type), PingRecord.Type.class);
        switch (ping.type) {
            case ENTITY -> buf.writeUuid(((EntityHitResult) ping.hitResult).getEntity().getUuid());
            case BLOCK, LOCATION -> buf.writeBlockHitResult((BlockHitResult) ping.hitResult);
        }
        ClientPlayNetworking.send(PacketList.CLIENT_PING, buf);
    }

    /**
     * <p>向服务器发送数据包：取消标记元素</p>
     * <p>该方法实际只是向服务器传输{@link PingRecord.Type}的数据</p>
     * @param ping 目标标记元素
     */
    public static void sendCancelPingPacket(@NotNull ClientPingRecord ping) {
        if (!ChaosRigApiClient.isServerExist()) {
            throw new RuntimeException("服务端不存在, 无法通过发送数据包添加Ping");
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumSet(EnumSet.of(ping.type), PingRecord.Type.class);
        ClientPlayNetworking.send(PacketList.CLIENT_CANCEL_PING, buf);
    }

    protected static void render(WorldRenderContext context) {
        Mouse mouse = MinecraftClient.getInstance().mouse;
        for (ClientPingRecord ping : getInstance().pings) {
            ping.render(context);
            if (mouse.isCursorLocked()) {
                checkCursorLockedFocusing(ping);
            } else {
                checkCursorUnlockedFocusing(ping);
            }
        }
    }

    protected static void checkCursorLockedFocusing(ClientPingRecord ping) {
        double angle = Vec3dHelper.ClientHelper.getCameraPosAngle(MinecraftClient.getInstance().gameRenderer.getCamera(), ping.getPos());
        if (angle >= 10.0f) {
            ping.onUnfocusing();
        } else {
            ping.onFocusing();
        }
    }

    protected static void checkCursorUnlockedFocusing(ClientPingRecord ping) {
        Vec3d endPos = Vec3dHelper.ClientHelper.getMousePointAt(ClientChaosRigApiConfig.pingMaxDistance);
        Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        double angle = Vec3dHelper.getPosAngle(new Vec3d(ping.getPos().x - cameraPos.x, ping.getPos().y - cameraPos.y, ping.getPos().z - cameraPos.z),
                new Vec3d(endPos.x - cameraPos.x, endPos.y - cameraPos.y, endPos.z - cameraPos.z));
        if (angle >= 10.0f) {
            ping.onUnfocusing();
        } else {
            ping.onFocusing();
        }
    }

    public boolean ping() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            ChaosRigApi.LOGGER.warn("[PingByPlayer] 当前会话Player实例为null");
            return false;
        }
        if (player.isSpectator()) {
            return false;
        }
        if (!new Box(MinecraftClient.getInstance().gameRenderer.getCamera().getBlockPos()).expand(1).intersects(player.getBoundingBox())) {
            player.sendMessage(Text.translatable("chaosrig.api.ping.camera.tip"), false);
            return false;
        }
        HitResult result = Vec3dHelper.getHitResult(player, MinecraftClient.getInstance().getTickDelta(), ClientChaosRigApiConfig.pingMaxDistance, false);
        if (result == null) {
            return false;
        }
        if (result.getType().equals(HitResult.Type.MISS)) {
            player.sendMessage(Text.translatable("chaosrig.api.ping.nothing"), true);
            return false;
        }
        if (pingAtCancel(result)) {
            return false;
        }
        return add(result);
    }

    public boolean pingByCamera() {
        if (!ChaosRigApiClient.isServerExist()) {
            ChaosRigApi.LOGGER.warn("当前仅客户端状态, 但调用了需要服务端的方法");
            return false;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            ChaosRigApi.LOGGER.warn("[PingByCamera] 当前会话Player实例为null");
            return false;
        }
        Camera playerCamera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d camPos = playerCamera.getPos();
        float pitchRadians = playerCamera.getPitch() * ((float) Math.PI / 180);
        float yawRadians = -playerCamera.getYaw() * ((float) Math.PI / 180);
        float yawCos = MathHelper.cos(yawRadians);
        float yawSin = MathHelper.sin(yawRadians);
        float pitchCos = MathHelper.cos(pitchRadians);
        float pitchSin = MathHelper.sin(pitchRadians);
        Vec3d camRo = new Vec3d(yawSin * pitchCos, -pitchSin, yawCos * pitchCos);
        HitResult result = Vec3dHelper.getHitResult(player, camPos, camRo, ClientChaosRigApiConfig.pingMaxDistance, false);
        if (result == null) {
            return false;
        }
        if (result.getType() == HitResult.Type.MISS) {
            player.sendMessage(Text.translatable("chaosrig.api.ping.nothing"), true);
            return false;
        }
        if (pingAtCancel(result)) {
            return false;
        }
        return add(result);
    }

    public boolean regroup() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            ChaosRigApi.LOGGER.warn("[Regroup] 当前会话Player实例为null");
            return false;
        }
        if (player.isSpectator()) {
            return false;
        }
        ClientRegroupPingRecord ping = new ClientRegroupPingRecord(player, ServerChaosRigApiConfig.pingRegroupAliveMaxTick);
        if (ChaosRigApiClient.isServerExist()) {
            sendPingPacket(ping);
        } else {
            addAtArray(ping);
        }
        return true;
    }

    public boolean addByOthers(@NotNull HitResult result) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            ChaosRigApi.LOGGER.warn("[PingByOthers] 当前会话Player实例为null");
            return false;
        }
        if (player.isSpectator()) {
            return false;
        }
        if (result.getType().equals(HitResult.Type.MISS)) {
            player.sendMessage(Text.translatable("chaosrig.api.ping.nothing"), true);
            return false;
        }
        if (pingAtCancel(result)) {
            return false;
        }
        return add(result);
    }

    protected boolean add(@NotNull HitResult result) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        if (result instanceof EntityHitResult entityHitResult) {
            ClientPingRecord ping = new ClientEntityPingRecord(player, entityHitResult, ServerChaosRigApiConfig.pingEntityAliveDefaultMaxTick);
            if (ChaosRigApiClient.isServerExist()) sendPingPacket(ping);
            else addAtArray(ping);
            return true;
        }
        if (result instanceof BlockHitResult blockHitResult) {
            ClientWorld world = MinecraftClient.getInstance().world;
            ClientPingRecord ping = (world != null && world.getBlockEntity(blockHitResult.getBlockPos()) != null)
                    ? new ClientBlockPingRecord(player, blockHitResult, ServerChaosRigApiConfig.pingBlockAliveMaxTick)
                    : new ClientPlacePingRecord(player, blockHitResult, ServerChaosRigApiConfig.pingLocationAliveMaxTick);
            if (ChaosRigApiClient.isServerExist()) sendPingPacket(ping);
            else addAtArray(ping);
            return true;
        }
        return false;
    }

    protected void addAtArray(@NotNull ClientPingRecord ping) {
        pings.stream().filter(ping::isSameType).findAny().ifPresentOrElse(oldPing -> PingRecord.copy(ping, oldPing), () -> pings.add(ping));
    }

    public boolean pingAtCancel(@NotNull HitResult result) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        Box pingBox = new Box(result.getPos().subtract(1, 1, 1), result.getPos().add(1, 1, 1));
        for (ClientPingRecord ping : pings) {
            if (!ping.getOwner().getUuid().equals(player.getUuid())) {
                continue;
            }
            Box targetBox = (ping.hitResult instanceof EntityHitResult entityHitResult)
                    ? entityHitResult.getEntity().getBoundingBox()
                    : new Box(ping.getPos().subtract(0.5, 0.5, 0.5), ping.getPos().add(0.5, 0.5, 0.5));
            if (pingBox.intersects(targetBox)) {
                if (ChaosRigApiClient.isServerExist()) {
                    sendCancelPingPacket(ping);
                } else {
                    pings.remove(ping);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public final void syncData(@Nullable World world) {
        throw new RuntimeException("客户端无需向服务器同步自身数据");
    }

    @Override
    public final void markShouldSync() {
        throw new RuntimeException("客户端无需向服务器同步自身数据");
    }

    @Override
    public final boolean shouldSync() {
        return false;
    }

    @Override
    public void tickUpdate(@Nullable World world) {
        Iterator<ClientPingRecord> pingsIterator = pings.iterator();
        while (pingsIterator.hasNext()) {
            ClientPingRecord ping = pingsIterator.next();
            if (ping.isCanceled()) {
                pingsIterator.remove();
                continue;
            }
            ping.tick();
        }
    }
}
