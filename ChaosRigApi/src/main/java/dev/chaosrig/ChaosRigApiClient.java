package dev.chaosrig;

import dev.chaosrig.utils.KeyboardInput;
import dev.chaosrig.utils.PacketList;
import dev.chaosrig.utils.ResourceHelper;
import dev.chaosrig.utils.config.ClientChaosRigApiConfig;
import dev.chaosrig.utils.config.ReceiveSyncAnnotation;
import dev.chaosrig.utils.data.InteractionManager;
import dev.chaosrig.utils.ping.PingRenderer;
import dev.chaosrig.utils.registry.ItemGuiRendererRegistry;
import dev.chaosrig.utils.renderer.InformationScreen;
import dev.chaosrig.utils.renderer.PostShaders;
import dev.chaosrig.utils.renderer.OutlineRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

@Environment(EnvType.CLIENT)
public final class ChaosRigApiClient implements ClientModInitializer {
    private static boolean serverExist = false;
    private static boolean init = false;

    public static InteractionManager clientInteractionManager = new InteractionManager(true);
    public static ItemGuiRendererRegistry itemGuiRendererRegistry = new ItemGuiRendererRegistry();

    @Override
    public void onInitializeClient() {
        registerChecker();
        register();
        init = true;
    }

    private static void registerChecker() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(PacketList.CLIENT_CHECKING_SERVER_EXIST)) {
                PacketByteBuf checking = PacketByteBufs.empty();
                ClientPlayNetworking.send(PacketList.CLIENT_CHECKING_SERVER_EXIST, checking);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverExist = false;
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketList.CLIENT_CHECKED_SERVER_IS_EXIST, (client, handler, buf, responseSender) -> {
            serverExist = true;
        });
    }

    /**
     * 服务端是否装载<code>ChaosRigApi</code>MOD
     * @return 是与否
     */
    public static boolean isServerExist() {
        return serverExist;
    }

    private static void register() {
        ClientTickEvents.START_WORLD_TICK.register(world -> clientInteractionManager.tick(world));
        clientInteractionManager.addConsumer(PingRenderer.getInstance());
        ResourceHelper.register();
        PingRenderer.register();
        InformationScreen.register();
        ClientChaosRigApiConfig.register();
        ReceiveSyncAnnotation.register();
        OutlineRenderer.register();
        KeyboardInput.register();
        PostShaders.register();
    }

    /**
     * 初始化阶段结束
     * @return 是与否
     */
    public static boolean isInit() {
        return init;
    }
}
