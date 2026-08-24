package dev.chaosrig.utils.config;

import dev.chaosrig.ChaosRigApiClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;

import java.io.File;

@Environment(EnvType.CLIENT)
public class ClientChaosRigApiConfig {
    public static final File FILE = new File("config/chaos_rig_client.json");
    public static final ConfigManager MANAGER = new ConfigManager(FILE);

    @SyncFromServer(type = SyncType.Integer)
    public static int pingMaxDistance;

    public static float vhsBarrelAmount = 0.055f;
    public static float vhsChromaAberration = 0.018f;
    public static float vhsChromaEdge = 1.4f;
    public static float vhsChromaSmear = 0.0009f;
    public static float vhsTrackSpeed = 0.40f;
    public static float vhsTrackWidth = 0.07f;
    public static float vhsTrackJitter = 0.0020f;
    public static float vhsTrackBright = 0f;
    public static float vhsFlickerAmount = 0.035f;
    public static float vhsScanlineStrength = 0.10f;
    public static float vhsGrainStrength = 0.035f;
    public static float vhsVignetteStrength = 0.18f;
    public static float vhsOffsetIntensity = 0f;
    public static float vhsNoiseIntensity = 0.0008f;
    public static float vhsBlurNear = 0.9985f;
    public static float vhsBlurFar = 0.9999f;
    public static float vhsBlurRadius = 0.004f;
    public static float vhsBlurSamples = 8.0f; // int recomment
    public static float vhsMaxColorBlur = 0.85f;

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
    }

    public static void check() {

    }
}
