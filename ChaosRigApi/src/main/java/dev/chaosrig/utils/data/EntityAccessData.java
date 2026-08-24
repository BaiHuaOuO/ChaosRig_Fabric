package dev.chaosrig.utils.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.chaosrig.ChaosRigApi;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityAccessData {
    public static Codec<EntityAccessData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.CODEC.fieldOf("owner").forGetter(EntityAccessData::getOwnerUuid),
            Uuids.CODEC.listOf().optionalFieldOf("access_entity", null).forGetter(EntityAccessData::getAccesses)
            ).apply(instance, EntityAccessData::new));
    @NotNull
    protected final UUID ownerUuid;
    @Nullable
    protected LivingEntity ownerInstance;
    @NotNull
    protected List<UUID> accessEntities = Collections.synchronizedList(new ArrayList<>());

    @Nullable
    public static LivingEntity findEntity(@NotNull World world, @NotNull UUID uuid) {
        for (PlayerEntity player : world.getPlayers()) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        if (world instanceof ClientWorld clientWorld) {
            for (Entity entity : clientWorld.getEntities()) {
                if (entity.getUuid().equals(uuid) && entity instanceof LivingEntity livingEntity) {
                    return livingEntity;
                }
            }
        }
        if (world instanceof ServerWorld serverWorld && serverWorld.getEntity(uuid) instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }

    @Nullable
    public static NbtCompound write(@NotNull EntityAccessData data) {
        NbtElement nbtData = CODEC.encodeStart(NbtOps.INSTANCE, data).resultOrPartial(ChaosRigApi.LOGGER::error).orElse(null);
        if (nbtData == null) {
            return null;
        }
        NbtCompound nbt = new NbtCompound();
        nbt.put("player_accesses", nbtData);
        return nbt;
    }

    @Nullable
    public static EntityAccessData read(@NotNull NbtCompound nbt) {
        if (!nbt.contains("player_accesses")) {
            return null;
        }
        NbtElement nbtData = nbt.get("player_accesses");
        return CODEC.parse(NbtOps.INSTANCE, nbtData).resultOrPartial(ChaosRigApi.LOGGER::error).orElse(null);
    }

    public EntityAccessData(@NotNull UUID owner) {
        this.ownerUuid = owner;
        this.accessEntities.add(ownerUuid);
    }

    public EntityAccessData(@NotNull UUID owner, @Nullable List<UUID> accessEntities) {
        this(owner);
        if (accessEntities != null) {
            Collections.copy(this.accessEntities, accessEntities);
        }
    }

    public void set(@NotNull List<UUID> targets) {
        Collections.copy(this.accessEntities, targets);
    }

    public void add(@NotNull UUID target) {
        this.accessEntities.add(target);
    }

    public void remove(@NotNull UUID target) {
        this.accessEntities.remove(target);
    }

    public boolean isOwner(@NotNull UUID somebody) {
        return somebody.equals(this.ownerUuid);
    }

    public boolean contains(@NotNull UUID target) {
        return this.accessEntities.contains(target);
    }

    @Nullable
    public LivingEntity findAccess(@NotNull World world, @NotNull UUID target) {
        if (!this.contains(target)) {
            return null;
        }
        Entity entity = findEntity(world, target);
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }

    @NotNull
    public List<LivingEntity> getAccesses(@NotNull World world) {
        return this.accessEntities.stream().map(uuid -> findEntity(world, uuid)).filter(Objects::nonNull).toList();
    }

    @NotNull
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @NotNull
    public List<UUID> getAccesses() {
        return this.accessEntities;
    }

    @Nullable
    public LivingEntity getOwner(@Nullable World world) {
        if (world == null) {
            return this.ownerInstance;
        }
        if (this.ownerInstance == null) {
            this.ownerInstance = findEntity(world, this.ownerUuid);
        }
        return this.ownerInstance;
    }

    public boolean setOwner(@NotNull LivingEntity owner) {
        if (!owner.getUuid().equals(this.ownerUuid)) {
            return false;
        }
        this.ownerInstance = owner;
        return true;
    }
}
