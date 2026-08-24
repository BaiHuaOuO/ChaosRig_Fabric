package dev.chaosrig.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.Item;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public interface ItemGuiRenderer<T extends Item> {

    @NotNull
    ModelIdentifier getGuiTexture();

    @NotNull
    ModelIdentifier getHandTexture();

    @NotNull
    T getItem();

    @NotNull
    default ModelTransformationMode[] shouldRenderGuiTextureModes() {
        return new ModelTransformationMode[] {ModelTransformationMode.GUI, ModelTransformationMode.GROUND, ModelTransformationMode.FIXED};
    }

}
