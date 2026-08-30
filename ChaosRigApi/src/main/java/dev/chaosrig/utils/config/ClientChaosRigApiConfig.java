package dev.chaosrig.utils.config;

import dev.chaosrig.ChaosRigApiClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;

import java.io.File;

@Environment(EnvType.CLIENT)
public class ClientChaosRigApiConfig implements AutoCloseable {
    public static final File FILE = new File("config/chaos_rig_client.json");
    public static final ConfigManager MANAGER = new ConfigManager(FILE);

    /**
     * Ping功能最大允许距离
     */
    @SyncFromServer(type = SyncType.Integer) public static int pingMaxDistance;
    /**
     * 团结之力最大容忍距离
     */
    @SyncFromServer(type = SyncType.Integer) public static int stayTogetherMaxDistance;
    /**
     * 团结之力机制启用
     */
    @SyncFromServer(type = SyncType.Boolean) public static boolean stayTogetherEnabled;

    public static void register() {
        if (ChaosRigApiClient.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        ReceiveSyncAnnotation.addConsumer(ClientChaosRigApiConfig.class);
        reset();
        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static void load() {
        MANAGER.load(r -> {

        });
        check();
    }

    public static void save() {
        check();
        MANAGER.save(w -> {

        });
    }


    public static void reset() {
        pingMaxDistance = ServerChaosRigApiConfig.pingMaxDistance;
        stayTogetherMaxDistance = ServerChaosRigApiConfig.stayTogetherMaxDistance;
        stayTogetherEnabled = ServerChaosRigApiConfig.stayTogetherEnabled;
    }

    public static void check() {

    }

    @Override
    public void close() throws Exception {
        MANAGER.close();
    }
}
