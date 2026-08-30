package dev.chaosrig.utils.config;

import dev.chaosrig.ChaosRigApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ServerChaosRigApiConfig implements AutoCloseable {
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
    /**
     * Ping一个方块实体的最大默认显示时间
     */
    public static int pingBlockAliveMaxTick;
    /**
     * 集合标记最大默认显示时间
     */
    public static int pingRegroupAliveMaxTick;
    /**
     * Ping功能最大允许距离
     */
    @SyncToClient(type = SyncType.Integer) public static int pingMaxDistance;
    /**
     * 团结之力最大容忍距离
     */
    @SyncToClient(type = SyncType.Integer) public static int stayTogetherMaxDistance;
    /**
     * 团结之力机制启用
     */
    @SyncToClient(type = SyncType.Boolean) public static boolean stayTogetherEnabled;
    /**
     * 团结之力一秒一次的伤害值
     */
    public static int stayTogetherDamage;
    /**
     * 团结之力超出距离后的伤害伤害
     */
    public static int stayTogetherDamagingDelay;

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
        pingRegroupAliveMaxTick = 1200;
        pingMaxDistance = 128;
        stayTogetherMaxDistance = 12;
        stayTogetherEnabled = false;
        stayTogetherDamage = 1;
        stayTogetherDamagingDelay = 3;
    }

    public static void load() {
        check();
        MANAGER.load(r -> {
            pingLocationAliveMaxTick = r.onInteger("ping.location_alive_max_tick", pingLocationAliveMaxTick);
            pingEntityAliveDefaultMaxTick = r.onInteger("ping.entity_alive_default_max_tick", pingEntityAliveDefaultMaxTick);
            pingBlockAliveMaxTick = r.onInteger("ping.block_alive_max_tick", pingBlockAliveMaxTick);
            pingRegroupAliveMaxTick = r.onInteger("ping.regroup_alive_max_tick", pingRegroupAliveMaxTick);
            pingMaxDistance = r.onInteger("ping.max_distance", pingMaxDistance);
            stayTogetherEnabled = r.onBoolean("func.stay_together.enable", stayTogetherEnabled);
            stayTogetherMaxDistance = r.onInteger("func.stay_together.max_distance", stayTogetherMaxDistance);
            stayTogetherDamage = r.onInteger("func.stay_together.damage", stayTogetherDamage);
            stayTogetherDamagingDelay = r.onInteger("func.stay_together.damaging_delay", stayTogetherDamagingDelay);
        });
    }

    public static void save(@Nullable MinecraftServer server, boolean needSync) {
        check();
        MANAGER.save(w -> {
            w.onInteger("ping.location_alive_max_tick", pingLocationAliveMaxTick);
            w.onInteger("ping.entity_alive_default_max_tick", pingEntityAliveDefaultMaxTick);
            w.onInteger("ping.block_alive_max_tick", pingBlockAliveMaxTick);
            w.onInteger("ping.regroup_alive_max_tick", pingRegroupAliveMaxTick);
            w.onInteger("ping.max_distance", pingMaxDistance);
            w.onBoolean("func.stay_together.enable", stayTogetherEnabled);
            w.onInteger("func.stay_together.max_distance", stayTogetherMaxDistance);
            w.onInteger("func.stay_together.damage", stayTogetherDamage);
            w.onInteger("func.stay_together.damaging_delay", stayTogetherDamagingDelay);
        });
        if (needSync && server != null) {
            ProcessSyncAnnotation.init();
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
        if (pingBlockAliveMaxTick <= 0) {
            ChaosRigApi.LOGGER.warn("[Ping]标记方块最大存在时间必须为正整数");
            pingLocationAliveMaxTick = 900;
        }
        if (pingLocationAliveMaxTick <= 0) {
            ChaosRigApi.LOGGER.warn("[Ping]标记位置信息最大存在时间必须为正整数");
            pingLocationAliveMaxTick = 1200;
        }
        if (pingRegroupAliveMaxTick <= 0) {
            ChaosRigApi.LOGGER.warn("[Ping]集合标记最大存在时间必须为正整数");
            pingRegroupAliveMaxTick = 1200;
        }
        if (pingEntityAliveDefaultMaxTick <= 0) {
            ChaosRigApi.LOGGER.warn("[Ping]标记实体最大存在时间必须为正整数");
            pingEntityAliveDefaultMaxTick = 200;
        }
        if (stayTogetherMaxDistance <= 7) {
            ChaosRigApi.LOGGER.warn("[StayTogether]团结之力最大距离必须为8以上");
            stayTogetherMaxDistance = 12;
        }
        if (stayTogetherDamage <= 0) {
            ChaosRigApi.LOGGER.warn("[StayTogether]团结之力超距离伤害必须为0以上");
            stayTogetherDamage = 1;
        }
        if (stayTogetherDamagingDelay <= 0 || stayTogetherDamagingDelay >= 10) {
            ChaosRigApi.LOGGER.warn("[StayTogether]团结之力伤害延迟必须在区间[]");
            stayTogetherDamagingDelay = 3;
        }
    }

    @Override
    public void close() throws Exception {
        MANAGER.close();
    }
}

