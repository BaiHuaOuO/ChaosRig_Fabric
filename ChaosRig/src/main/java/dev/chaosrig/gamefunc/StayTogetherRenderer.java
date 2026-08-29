package dev.chaosrig.gamefunc;

import dev.chaosrig.utils.config.ClientChaosRigApiConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.GameMode;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class StayTogetherRenderer {
    protected static double closetSquaredDistance = 0;
    protected static boolean save = true;
    protected static boolean hasTeammate = false;
    protected static short tick = 0;

    public static void register() {
        ClientTickEvents.END_WORLD_TICK.register(StayTogetherRenderer::tick);
    }

    protected static void tick(ClientWorld world) {
        if (!ClientChaosRigApiConfig.stayTogetherEnabled) {
            return;
        }
        tick++;
        if (tick >= 10) {
            resetDistance();
            tick = 0;
            process(world);
            warning();
        }
    }

    protected static void warning() {
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self == null) {
            return;
        }
        if (self.isSpectator() || self.isCreative()) {
            return;
        }
        if (!hasTeammate) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        double maxSquaredDistance = ClientChaosRigApiConfig.stayTogetherMaxDistance * ClientChaosRigApiConfig.stayTogetherMaxDistance;
        double minSquaredDistance = maxSquaredDistance * 0.5;
        if (save) {
            if (closetSquaredDistance >= minSquaredDistance && closetSquaredDistance <= maxSquaredDistance) {
                float v = Math.max(0.2f, (int) (closetSquaredDistance - minSquaredDistance / maxSquaredDistance - minSquaredDistance));
                client.getSoundManager().play(new PositionedSoundInstance(SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1f, v, SoundInstance.createRandom(), client.player.getBlockPos()));
            }
        } else {
            if (closetSquaredDistance >= maxSquaredDistance) {
                client.getSoundManager().play(new PositionedSoundInstance(SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1f, 1f, SoundInstance.createRandom(), client.player.getBlockPos()));
            }
        }
    }

    protected static void process(ClientWorld world) {
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self == null) {
            return;
        }
        if (self.isSpectator() || self.isCreative()) {
            return;
        }
        double maxSquaredDistance = ClientChaosRigApiConfig.stayTogetherMaxDistance * ClientChaosRigApiConfig.stayTogetherMaxDistance;
        hasTeammate = false;
        for (PlayerListEntry other : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
            UUID otherUuid = other.getProfile().getId();
            if (otherUuid.equals(self.getUuid())) {
                continue;
            }
            if (other.getGameMode() == GameMode.CREATIVE || other.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            boolean isTeammate = false;
            if (self.getScoreboardTeam() == null && other.getScoreboardTeam() == null) {
                isTeammate = true;
                hasTeammate = true;
            }
            if (self.getScoreboardTeam() != null) {
                if (self.getScoreboardTeam().equals(other.getScoreboardTeam())) {
                    isTeammate = true;
                    hasTeammate = true;
                }
            }
            if (!isTeammate) {
                continue;
            }
            AbstractClientPlayerEntity target = world.getPlayers().stream().filter(p -> p.getUuid().equals(otherUuid)).findFirst().orElse(null);
            if (target == null) {
                continue;
            }
            double distance = target.getPos().squaredDistanceTo(self.getPos());
            if (distance <= closetSquaredDistance) {
                closetSquaredDistance = distance;
            }
        }
        if (!hasTeammate) {
            return;
        }
        save = closetSquaredDistance <= maxSquaredDistance;
    }

    public static boolean isHasTeammate() {
        return hasTeammate;
    }

    public static boolean isSave() {
        return save;
    }

    public static boolean isEnable() {
        return ClientChaosRigApiConfig.stayTogetherEnabled;
    }

    public static void resetDistance() {
        closetSquaredDistance = MinecraftClient.getInstance().options.getViewDistance().getValue() * 16 + 4;
        closetSquaredDistance = closetSquaredDistance * closetSquaredDistance;
        save = true;
    }
}
