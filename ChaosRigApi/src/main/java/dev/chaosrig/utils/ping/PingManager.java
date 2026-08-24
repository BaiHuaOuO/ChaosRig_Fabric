package dev.chaosrig.utils.ping;

import dev.chaosrig.ChaosRigApi;
import dev.chaosrig.utils.ColorTools;
import dev.chaosrig.utils.PacketList;
import dev.chaosrig.utils.data.DataConsumer;
import dev.chaosrig.utils.renderer.InformationScreen;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PingManager extends DataConsumer {
    protected final Map<PingRecord, DimensionType> pings = new ConcurrentHashMap<>();
    protected final MinecraftServer server;

    public PingManager(@NotNull MinecraftServer server) {
        super(false);
        this.server = server;
        InformationScreen.push("[server] pings", -1, ColorTools.WHITE.apply(255), () -> this.pings.keySet().toString());
    }

    public static void register() {
        if (ChaosRigApi.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        ServerPlayNetworking.registerGlobalReceiver(PacketList.CLIENT_PING, PingManager::fromClientRequirePing);
        ServerPlayNetworking.registerGlobalReceiver(PacketList.CLIENT_CANCEL_PING, PingManager::fromClientRequireCancelPing);
    }

    protected static void fromClientRequirePing(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        ChaosRigApi.serverInteractionManager.find(PingManager.class).forEach(m -> {
            PingRecord.Type type = buf.readEnumSet(PingRecord.Type.class).stream().findFirst().orElseThrow();
            switch (type) {
                case ENTITY -> {
                    Entity entity = player.getServerWorld().getEntity(buf.readUuid());
                    if (entity == null) return;
                    m.addEntity(player, entity);
                }
                case BLOCK -> m.addBlock(player, buf.readBlockHitResult());
                default -> m.addPlace(player, buf.readBlockHitResult());
            }
        });
    }

    protected static void fromClientRequireCancelPing(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        ChaosRigApi.serverInteractionManager.find(PingManager.class).forEach(m -> {
            m.cancel(player, buf.readEnumSet(PingRecord.Type.class).stream().findFirst().orElseThrow());
        });
    }

    public void cancel(@NotNull LivingEntity sender, @NotNull PingRecord.Type type) {
        ServerWorld world = server.getWorld(sender.getWorld().getRegistryKey());
        if (world == null) return;
        for (Map.Entry<PingRecord, DimensionType> set : pings.entrySet()) {
            if (!set.getKey().type.equals(type)) continue;
            if (!set.getKey().owner.getUuid().equals(sender.getUuid())) continue;
            set.getKey().setCancel();
            this.markShouldSync();
            break;
        }
    }

    public final void addPlace(@NotNull LivingEntity sender, @NotNull BlockHitResult result) {
        this.add(sender, new PingRecord.PlacePingRecord(sender, result));
    }

    public final void addBlock(@NotNull LivingEntity sender, @NotNull BlockHitResult result) {
        this.add(sender, new PingRecord.BlockPingRecord(sender, result));
    }

    public final void addEntity(@NotNull LivingEntity sender, @NotNull Entity target) {
        this.add(sender, new PingRecord.EntityPingRecord(sender, new EntityHitResult(target)));
    }

    public void add(@NotNull LivingEntity sender, PingRecord ping) {
        ServerWorld world = this.server.getWorld(sender.getWorld().getRegistryKey());
        if (world == null) return;
        pings.keySet().removeIf(ping::isSameType);
        pings.put(ping, sender.getEntityWorld().getDimension());
        this.markShouldSync();
    }

    @Override
    public void syncData(@Nullable World world) {
        PacketByteBuf buf = PacketByteBufs.create();
        boolean isEmpty = pings.isEmpty();
        buf.writeBoolean(isEmpty);
        if (!isEmpty) {
            Set<PingRecord> tmpPings = new HashSet<>(pings.keySet());
            buf.writeCollection(tmpPings, (packetBuf, ping) -> {
                packetBuf.writeUuid(ping.owner.getUuid());
                packetBuf.writeEnumSet(EnumSet.of(ping.type), PingRecord.Type.class);
                packetBuf.writeInt(ping.tick);
                packetBuf.writeInt(ping.maxTick);
                if (ping.type == PingRecord.Type.ENTITY) {
                    EntityHitResult result = (EntityHitResult) ping.hitResult;
                    packetBuf.writeUuid(result.getEntity().getUuid());
                } else {
                    packetBuf.writeBlockHitResult((BlockHitResult) ping.hitResult);
                }
            });
        }
        for (ServerPlayerEntity player : this.server.getPlayerManager().getPlayerList()) {
            if (pings.isEmpty()) {
                ServerPlayNetworking.send(player, PacketList.SERVER_SYNC_PING_DATA, buf);
            } else {
                for (Map.Entry<PingRecord, DimensionType> copy : pings.entrySet()) {
                    if (!player.getEntityWorld().getDimension().equals(copy.getValue())) continue;
                    ServerPlayNetworking.send(player, PacketList.SERVER_SYNC_PING_DATA, buf);
                }
            }
        }
        this.markSynced(world);
    }

    @Override
    public void tickUpdate(@Nullable World world) {
        Iterator<PingRecord> pingsIterator = pings.keySet().iterator();
        boolean sync = false;
        while (pingsIterator.hasNext()) {
            PingRecord ping = pingsIterator.next();
            if (ping.isCanceled()) {
                pingsIterator.remove();
                sync = true;
                continue;
            }
            ping.tick();
        }
        if (sync) {
            this.markShouldSync();
        }
    }
}