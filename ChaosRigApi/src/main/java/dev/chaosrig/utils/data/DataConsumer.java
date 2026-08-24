package dev.chaosrig.utils.data;

import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class DataConsumer implements InteractionConsumer {
    protected final boolean isClient;
    protected boolean shouldSync = false;
    
    public DataConsumer(boolean isClient) {
        this.isClient = isClient;
    }

    @Override
    public boolean isClient() {
        return this.isClient;
    }

    @Override
    public boolean shouldSync() {
        return this.shouldSync;
    }

    @Override
    public void markShouldSync() {
        this.shouldSync = true;
    }

    protected void markSynced(@Nullable World world) {
        this.shouldSync = false;
    }
}
