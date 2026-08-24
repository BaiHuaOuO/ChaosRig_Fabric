package dev.chaosrig.item.mixin;

import dev.chaosrig.ChaosRigApiClient;
import dev.chaosrig.item.ItemGuiRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Shadow
    public abstract ItemModels getModels();

    @ModifyVariable(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER),
        argsOnly = true)
    private BakedModel onRenderItem(BakedModel model, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ItemGuiRenderer<?> itemGuiRenderer = ChaosRigApiClient.itemGuiRendererRegistry.getOnly(stack.getItem());
        if (itemGuiRenderer != null) {
            for (ModelTransformationMode allowMode : itemGuiRenderer.shouldRenderGuiTextureModes()) {
                if (allowMode == renderMode) {
                    return this.getModels().getModelManager().getModel(itemGuiRenderer.getGuiTexture());
                }
            }
        }
        return model;
    }
}
