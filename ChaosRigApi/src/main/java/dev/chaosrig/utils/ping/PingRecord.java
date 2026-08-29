package dev.chaosrig.utils.ping;

import dev.chaosrig.utils.config.ServerChaosRigApiConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class PingRecord {
    @NotNull
    protected final Type type;
    @NotNull
    protected final LivingEntity owner;
    @NotNull
    protected HitResult hitResult;
    protected int maxTick;
    protected int tick = 0;
    protected volatile boolean canceled = false;

    public PingRecord(@NotNull Type type, @NotNull LivingEntity owner, @NotNull HitResult hitResult, int maxTick) {
        this.type = type;
        this.owner = owner;
        if (type == Type.ENTITY && !(hitResult instanceof EntityHitResult)) {
            throw new RuntimeException("传参类型为实体, 但是提供的HitResult不为EntityHitResult");
        }
        this.hitResult = hitResult;
        this.maxTick = maxTick;
    }

    public static void copy(PingRecord from, PingRecord to) {
        to.maxTick = from.maxTick;
        to.tick = from.tick;
        to.canceled = from.canceled;
        to.hitResult = from.hitResult;
    }

    protected void onTick() {}

    public final void tick() {
        tick++;
        if (tick > maxTick) {
            this.setCancel();
            return;
        }
        this.onTick();
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @NotNull
    public LivingEntity getOwner() {
        return owner;
    }

    @NotNull
    public HitResult getHitResult() {
        return hitResult;
    }

    public boolean isCanceled() {
        if (this.tick > this.maxTick) {
            this.canceled = true;
        }
        return this.canceled;
    }

    public int getMaxTick() {
        return this.maxTick;
    }

    public int getTick() {
        return this.tick;
    }

    public void setCancel() {
        this.canceled = true;
        this.tick = this.maxTick + 1;
    }

    @NotNull
    public Vec3d getPos() {
        return this.hitResult.getPos();
    }

    public boolean isSameType(PingRecord ping) {
        return ping.type == this.type && ping.owner.getUuid().equals(this.owner.getUuid());
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PingRecord that)) return false;
        return maxTick == that.maxTick && type == that.type && owner.getUuid().equals(that.owner.getUuid());
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + owner.getUuid().hashCode();
        result = 31 * result + maxTick;
        return result;
    }

    @Override
    public String toString() {
        return "%s/%s[owner: %s | type: %s | pos: %s]".formatted(this.tick, this.maxTick, this.getOwner().getUuid(), this.type, this.hitResult.getPos());
    }

    public enum Type {
        ENTITY,
        LOCATION,
        BLOCK,
        REGROUP;
    }

    public enum Relationship {
        DEFAULT,
        TEAMMATE,
        ENEMY
    }

    public static class PlacePingRecord extends PingRecord {

        public PlacePingRecord(@NotNull LivingEntity owner, @NotNull BlockHitResult hitResult) {
            super(Type.LOCATION, owner, hitResult, ServerChaosRigApiConfig.pingLocationAliveMaxTick);
        }
    }

    public static class EntityPingRecord extends PingRecord {

        public EntityPingRecord(@NotNull LivingEntity owner, @NotNull EntityHitResult hitResult) {
            this(owner, hitResult, ServerChaosRigApiConfig.pingEntityAliveDefaultMaxTick);
        }

        public EntityPingRecord(@NotNull LivingEntity owner, @NotNull EntityHitResult hitResult, int maxTick) {
            super(Type.ENTITY, owner, hitResult, maxTick);
        }

        @Override
        protected void onTick() {
            if (((EntityHitResult) this.hitResult).getEntity() instanceof LivingEntity livingEntity) {
                if (livingEntity.isDead()) {
                    this.setCancel();
                    return;
                }
            }
            if (((EntityHitResult) this.hitResult).getEntity().isInvisible()) {
                this.setCancel();
            }
        }

        @Override
        public @NotNull Vec3d getPos() {
            return ((EntityHitResult) this.hitResult).getEntity().getPos();
        }
    }

    public static class BlockPingRecord extends PingRecord {

        public BlockPingRecord(@NotNull LivingEntity owner, @NotNull BlockHitResult hitResult, int maxTick) {
            super(Type.BLOCK, owner, hitResult, maxTick);
        }

        public BlockPingRecord(@NotNull LivingEntity owner, @NotNull BlockHitResult hitResult) {
            super(Type.BLOCK, owner, hitResult, ServerChaosRigApiConfig.pingBlockAliveMaxTick);
        }

        @Override
        public @NotNull Vec3d getPos() {
            return ((BlockHitResult) this.hitResult).getBlockPos().toCenterPos();
        }

        @Override
        protected void onTick() {
            BlockState blockState = this.owner.getWorld().getBlockState(((BlockHitResult) this.hitResult).getBlockPos());
            if (blockState == null) {
                this.setCancel();
                return;
            }
            if (blockState.isAir()) {
                this.setCancel();
                return;
            }
        }
    }

    public static class RegroupPingRecord extends PingRecord {

        public RegroupPingRecord(@NotNull LivingEntity owner, int maxTick) {
            super(Type.REGROUP, owner, new BlockHitResult(owner.getPos(), Direction.UP, owner.getBlockPos(), false), maxTick);
        }

        public RegroupPingRecord(@NotNull LivingEntity owner) {
            super(Type.REGROUP, owner, new BlockHitResult(owner.getPos(), Direction.UP, owner.getBlockPos(), false), ServerChaosRigApiConfig.pingRegroupAliveMaxTick);
        }

        @Override
        public @NotNull Vec3d getPos() {
            return ((BlockHitResult) this.hitResult).getBlockPos().toCenterPos();
        }
    }
}
