package dev.chaosrig.block;

import dev.chaosrig.utils.TooltipReader;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class ChaosRigBlock extends Block {

    public ChaosRigBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        if (Screen.hasShiftDown()) {
            tooltip.addAll(Arrays.asList(TooltipReader.read(this)));
        } else {
            tooltip.add(TooltipReader.readWithInfo(this));
            tooltip.add(Text.translatable("[Shift] to read detials"));
        }
    }
}
