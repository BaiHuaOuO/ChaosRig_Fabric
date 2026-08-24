package dev.chaosrig.utils.config;

import dev.chaosrig.ChaosRigApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ServerChaosRigApiConfig {
    public static final File FILE = new File("config/chaos_rig_server.json");
    public static final ConfigManager MANAGER = new ConfigManager(FILE);
    /**
     * Ping一处位置的最大显示时间
     */
    public static int pingLocationAliveMaxTick;
    /**
     * Ping一个实体的默认最大显示时间
     */
    public static int pingEntityAliveDefaultMaxTick;
    public static int pingBlockAliveMaxTick;
    @SyncToClient(type = SyncType.Integer)
    public static int pingMaxDistance;

    public static void register() {
        if (ChaosRigApi.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        reset();
        ProcessSyncAnnotation.addProvider(ServerChaosRigApiConfig.class);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            save(server, false);
            reset();
        });
    }

    public static void reset() {
        pingLocationAliveMaxTick = 1200;
        pingEntityAliveDefaultMaxTick = 200;
        pingBlockAliveMaxTick = 900;
        pingMaxDistance = 128;
    }

    public static void load() {
        check();
        MANAGER.load(r -> {
            pingLocationAliveMaxTick = r.onInteger("ping.location_alive_max_tick", pingLocationAliveMaxTick);
            pingEntityAliveDefaultMaxTick = r.onInteger("ping.entity_alive_default_max_tick", pingEntityAliveDefaultMaxTick);
            pingBlockAliveMaxTick = r.onInteger("ping.block_alive_max_tick", pingBlockAliveMaxTick);
            pingMaxDistance = r.onInteger("ping.max_distance", pingMaxDistance);
        });
    }

    public static void save(@Nullable MinecraftServer server, boolean needSync) {
        check();
        MANAGER.save(w -> {
            w.onInteger("ping.location_alive_max_tick", pingLocationAliveMaxTick);
            w.onInteger("ping.entity_alive_default_max_tick", pingEntityAliveDefaultMaxTick);
            w.onInteger("ping.block_alive_max_tick", pingBlockAliveMaxTick);
            w.onInteger("ping.max_distance", pingMaxDistance);
        });
        ProcessSyncAnnotation.init();
        if (needSync && server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ProcessSyncAnnotation.send(player);
            }
        }
    }

    public static void check() {
        if (pingMaxDistance <= 5 || pingMaxDistance >= 1024) {
            ChaosRigApi.LOGGER.warn("[Ping]最大距离必须在区间[5, 1024]中");
            pingMaxDistance = 128;
        }
        if (pingBlockAliveMaxTick < 0) {
            ChaosRigApi.LOGGER.warn("[Ping]标记方块最大存在时间必须为正整数");
            pingLocationAliveMaxTick = 900;
        }
        if (pingLocationAliveMaxTick < 0) {
            ChaosRigApi.LOGGER.warn("[Ping]标记位置信息最大存在时间必须为正整数");
            pingLocationAliveMaxTick = 1200;
        }
        if (pingEntityAliveDefaultMaxTick < 0) {
            ChaosRigApi.LOGGER.warn("[Ping]标记实体最大存在时间必须为正整数");
            pingEntityAliveDefaultMaxTick = 200;
        }
    }
}

