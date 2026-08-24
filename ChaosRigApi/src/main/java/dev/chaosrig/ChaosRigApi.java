package dev.chaosrig;

import dev.chaosrig.utils.PacketList;
import dev.chaosrig.utils.config.ProcessSyncAnnotation;
import dev.chaosrig.utils.config.ServerChaosRigApiConfig;
import dev.chaosrig.utils.data.InteractionManager;
import dev.chaosrig.utils.ping.PingManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChaosRigApi implements ModInitializer {
    public static final String API_MOD_ID = "chaos_rig_api";
    public static final String MAIN_MOD_ID = "chaos_rig";
    public static final Logger LOGGER = LoggerFactory.getLogger(API_MOD_ID);
    private static boolean mainModExist = false;
    private static boolean init = false;

    public static InteractionManager serverInteractionManager = new InteractionManager(false);

    @Override
    public void onInitialize() {
        LOGGER.info("ChaosRig[Api] init...");
        register();
        registerReceiver();
        if (FabricLoader.getInstance().isModLoaded(MAIN_MOD_ID)) {
            mainModExist = true;
        }
        init = true;
    }

    private static void registerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(PacketList.CLIENT_CHECKING_SERVER_EXIST, (server, player, handler, buf, sender) -> {
            PacketByteBuf checked = PacketByteBufs.create();
            ServerPlayNetworking.send(player, PacketList.CLIENT_CHECKED_SERVER_IS_EXIST, checked);
        });
    }

    private static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> serverInteractionManager.tick(null));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            serverInteractionManager.addConsumer(new PingManager(server));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> serverInteractionManager.clear());
        PingManager.register();
        ServerChaosRigApiConfig.register();
        ProcessSyncAnnotation.register();
    }

    /**
     * <code>ChaosRig</code>MOD是否存在于<code>Fabric</code>环境中
     * @return 是与否
     */
    public static boolean isMainModExist() {
        return mainModExist;
    }

    /**
     * 初始化阶段结束
     * @return 是与否
     */
    public static boolean isInit() {
        return init;
    }
}
