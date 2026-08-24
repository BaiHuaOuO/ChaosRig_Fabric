package dev.chaosrig.utils.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class PersistentConsumer extends PersistentState implements InteractionConsumer {
    protected final boolean isClient;
    protected boolean shouldSync = false;

    public PersistentConsumer(boolean isClient) {
        this.isClient = isClient;
    }

    @Override
    public void markShouldSync() {
        this.shouldSync = true;
    }

    @Override
    public boolean shouldSync() {
        return this.shouldSync;
    }

    protected void markSynced(@Nullable World world) {
        this.shouldSync = false;
    }

    public void markUpdate() {
        this.markDirty();
        this.markShouldSync();
    }

    @Override
    public final boolean isClient() {
        return this.isClient;
    }

    public static <T extends PersistentConsumer> T create(@NotNull ServerWorld world,
                                                               @NotNull Identifier consumerId,
                                                               @NotNull Function<NbtCompound, T> loader,
                                                               @NotNull Supplier<T> builder) {
        PersistentStateManager stateManager = world.getPersistentStateManager();
        return stateManager.getOrCreate(loader, builder, consumerId.getNamespace() + '_' + consumerId.getPath());
    }
}
