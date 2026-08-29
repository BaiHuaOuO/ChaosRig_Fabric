package dev.chaosrig.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ConfigScreen extends SelectableScreen {

    public ConfigScreen(@NotNull Text title, @Nullable Screen parent) {
        super(title, parent);
    }

    @Override
    protected void initElement(ElementProvider provider) {
    }
}
