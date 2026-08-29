package dev.chaosrig.gamefunc;

import dev.chaosrig.ChaosRig;
import dev.chaosrig.utils.config.ServerChaosRigApiConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StayTogetherManager {
    protected static int initPlayerProtectingTick = 0;
    protected static Set<PlayerData> playerData = new HashSet<>();
    protected static Map<RegistryKey<World>, Position> positionProtecting = new HashMap<>(); // TODO: 时间 ~ 免疫区域
    protected static short tick = 0;

    public static void register() {
        if (ChaosRig.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        ServerTickEvents.END_SERVER_TICK.register(StayTogetherManager::tick);
    }

    protected static void tick(MinecraftServer server) {
        if (!ServerChaosRigApiConfig.stayTogetherEnabled) {
            return;
        }
        if (server.getPlayerManager().getPlayerList().size() <= 1) {
            initPlayerProtectingTick = 100;
            return;
        }
        if (initPlayerProtectingTick >= 0) {
            initPlayerProtectingTick--;
            return;
        }
        tick++;
        if (tick >= 20) {
            tick = 0;
            int maxDistance = ServerChaosRigApiConfig.stayTogetherMaxDistance * ServerChaosRigApiConfig.stayTogetherMaxDistance;
            for (ServerWorld world : server.getWorlds()) {
                process(world, maxDistance);
            }
        }
    }

    protected static void process(ServerWorld world, double maxDistance) {
        if (world.getPlayers().isEmpty()) {
            return;
        }
        processPlayers(world, maxDistance);
    }

    protected static void processPlayers(ServerWorld world, double maxDistance) {
        // 雷霆大if
        for (ServerPlayerEntity target : world.getPlayers()) {
            PlayerData data = getData(target);
            // avoid gamemode 1, 3
            if (target.isCreative() || target.isSpectator()) {
                data.setProtectingCountDown(ServerChaosRigApiConfig.stayTogetherDamagingDelay);
                data.setDamaged(false);
                continue;
            }
            boolean save = false;
            boolean hasTeammate = false; // check any teammate in Server but not in world
            for (ServerPlayerEntity other : world.getServer().getPlayerManager().getPlayerList()) {
                if (other.getUuid().equals(target.getUuid())) { // avoid self
                    continue;
                }
                if (other.isCreative() || other.isSpectator()) {
                    continue;
                }
                boolean shouldCheck = false;
                // check teammate
                if (target.getScoreboardTeam() == null && other.getScoreboardTeam() == null) {
                    shouldCheck = true;
                    hasTeammate = true;
                }
                if (target.getScoreboardTeam() != null && other.getScoreboardTeam() != null) {
                    if (target.isTeammate(other)) {
                        shouldCheck = true;
                        hasTeammate = true;
                    }
                }
                // check world and distance
                if (shouldCheck && world.getPlayers().contains(other) && other.getPos().squaredDistanceTo(target.getPos()) <= maxDistance) {
                    save = true;
                }
            }
            if (save || !hasTeammate) {
                data.setDamaged(false);
                data.setProtectingCountDown(ServerChaosRigApiConfig.stayTogetherDamagingDelay);
            } else {
                if (data.isProtecting()) {
                    data.setDamaged(false);
                    data.countDown();
                    continue;
                }
                if (!data.isProtecting() && includedProtectingAreas(target, world, maxDistance)) {
                    data.setDamaged(false);
                    data.setProtectingCountDown(ServerChaosRigApiConfig.stayTogetherDamagingDelay);
                    continue;
                }
                data.setDamaged(true);
            }
        }
    }

    protected static boolean includedProtectingAreas(ServerPlayerEntity target, ServerWorld world, double maxDistance) {
        if (positionProtecting.isEmpty()) {
            return false;
        }
        List<BlockPos> protectingAreas = positionProtecting.get(world.getRegistryKey()).positions();
        if (protectingAreas.isEmpty()) {
            return false;
        }
        for (BlockPos pos : protectingAreas) {
            if (pos.getSquaredDistance(target.getPos()) <= maxDistance) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static PlayerData getData(@NotNull ServerPlayerEntity target) {
        PlayerData data = playerData.stream().filter(d -> d.owner.getUuid().equals(target.getUuid())).findFirst().orElse(null);
        if (data == null) {
            data = new PlayerData(target);
            playerData.add(data);
        }
        return data;
    }

    public record Position(List<BlockPos> positions) {}

    public static class PlayerData {
        protected final ServerPlayerEntity owner;
        protected int protectingCountDown = ServerChaosRigApiConfig.stayTogetherDamagingDelay;
        protected boolean damaged = false;

        protected PlayerData(@NotNull ServerPlayerEntity owner, int protectingCountDown) {
            this.owner = owner;
            this.protectingCountDown = protectingCountDown;
        }

        public PlayerData(@NotNull ServerPlayerEntity owner) {
            this.owner = owner;
        }

        public void setDamaged(boolean damaged) {
            if (this.owner.isDead()) {
                return;
            }
            if (damaged) {
                Random random = new Random();
                owner.getWorld().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ENTITY_PLAYER_HURT, owner.getSoundCategory(), (random.nextFloat() - random.nextFloat()) * 0.2f + 1.5f, 1.0f);
                owner.setHealth(owner.getHealth() - ServerChaosRigApiConfig.stayTogetherDamage);
            }
            this.damaged = damaged;
        }

        public boolean isDamaged() {
            return damaged;
        }

        public void countDown() {
            protectingCountDown--;
        }

        public void setProtectingCountDown(int protectingCountDown) {
            this.protectingCountDown = protectingCountDown;
        }

        public boolean isProtecting() {
            return protectingCountDown > 0;
        }

        @Override
        public String toString() {
            return "Owner: %s | damaged: %s | protecting: %s".formatted(owner.getName(), damaged, protectingCountDown);
        }
    }
}
