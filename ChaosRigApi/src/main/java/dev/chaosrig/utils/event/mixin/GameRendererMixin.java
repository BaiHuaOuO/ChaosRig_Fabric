package dev.chaosrig.utils.event.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.chaosrig.utils.event.GameRendererEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    MinecraftClient client;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;draw()V", shift = At.Shift.AFTER))
    private void onRender(float tickDelta, long startTime, boolean tick, CallbackInfo ci, @Local DrawContext drawContext) {
        GameRendererEvent.TAIL.invoker().onRender((GameRenderer) ((Object) this), this.client, drawContext, tickDelta, startTime, tick);
        drawContext.draw();
    }
}
