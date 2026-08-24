package dev.chaosrig.item;

import dev.chaosrig.utils.TooltipReader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class ChaosRigItem extends Item {

    public ChaosRigItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (Screen.hasShiftDown()) {
            tooltip.addAll(Arrays.asList(TooltipReader.read(this)));
        } else {
            tooltip.add(TooltipReader.readWithInfo(this));
            tooltip.add(Text.translatable("[Shift] to read detials"));
        }
    }
}
