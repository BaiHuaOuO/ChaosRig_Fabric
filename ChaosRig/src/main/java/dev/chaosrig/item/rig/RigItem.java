package dev.chaosrig.item.rig;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RigItem extends Item implements DefaultRig {

    public RigItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean deplete(@NotNull LivingEntity entity, @NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int depleteNeeds(@NotNull ItemStack stack) {
        return 0;
    }
}
