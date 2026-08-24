package dev.chaosrig.item.rig;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface DefaultRig {
    String NBT_KEY = "rig_ability";

    default boolean isUnstable() {
        return false;
    }

    default int canUseTimes() {
        return 0;
    }

    default boolean canDeplete(@NotNull LivingEntity entity, @NotNull ItemStack stack) {
        return true;
    }

    boolean deplete(@NotNull LivingEntity entity, @NotNull ItemStack stack);

    int depleteNeeds(@NotNull ItemStack stack);
}