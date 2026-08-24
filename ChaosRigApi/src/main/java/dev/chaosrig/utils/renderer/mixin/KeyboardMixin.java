package dev.chaosrig.utils.renderer.mixin;

import dev.chaosrig.utils.renderer.HudOverlay;
import dev.chaosrig.utils.renderer.InformationScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        InformationScreen.KEYBOARD_EVENT.invoker().onInput(window, key, scancode, action, modifiers);
        if (this.client.getOverlay() instanceof HudOverlay hudOverlay) {
            boolean[] lock = new boolean[] {false};
            HudOverlay.wrapScreenError(() -> {
                if (action == 1 || action == 2) {
                    lock[0] = hudOverlay.keyPressed(key, scancode, modifiers);
                } else if(action == 0) {
                    lock[0] = hudOverlay.keyReleased(key, scancode, modifiers);
                }
            }, "keyPressed event handler", hudOverlay.getClass().getCanonicalName());
            if (lock[0]) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"))
    private void onChar(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (window != this.client.getWindow().getHandle()) {
            return;
        }
        if (this.client.getOverlay() instanceof HudOverlay hudOverlay) {
            if (Character.charCount(codePoint) == 1) {
                HudOverlay.wrapScreenError(() -> hudOverlay.charTyped((char)codePoint, modifiers), "charTyped event handler", hudOverlay.getClass().getCanonicalName());
            } else {
                for (char c : Character.toChars(codePoint)) {
                    HudOverlay.wrapScreenError(() -> hudOverlay.charTyped(c, modifiers), "charTyped event handler", hudOverlay.getClass().getCanonicalName());
                }
            }
        }
    }
}
