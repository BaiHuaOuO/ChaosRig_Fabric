package dev.chaosrig.utils.renderer.mixin;

import dev.chaosrig.utils.renderer.HudOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void lockCursor();

    @Shadow
    private boolean cursorLocked;

    @Shadow
    private double y;

    @Shadow
    private double x;

    @Shadow
    public abstract void unlockCursor();

    @Shadow
    private int activeButton;

    @Shadow
    private double glfwTime;

    @Inject(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;", shift = At.Shift.BEFORE, ordinal = 0))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (this.client.getOverlay() instanceof HudOverlay hudOverlay) {
            double mouseX = this.x * (double) this.client.getWindow().getScaledWidth() / (double) this.client.getWindow().getWidth();
            double mouseY = this.y * (double) this.client.getWindow().getScaledHeight() / (double) this.client.getWindow().getHeight();
            if (action == 1) {
                HudOverlay.wrapScreenError(() -> {
                    hudOverlay.mouseClicked(mouseX, mouseY, button);
                }, "mouseClicked event handler", hudOverlay.getClass().getCanonicalName());
            } else {
                HudOverlay.wrapScreenError(() -> {
                    hudOverlay.mouseReleased(mouseX, mouseY, button);
                }, "mouseReleased event handler", hudOverlay.getClass().getCanonicalName());
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == this.client.getWindow().getHandle()) {
            if (this.client.getOverlay() instanceof HudOverlay hudOverlay) {
                double amount = (this.client.options.getDiscreteMouseScroll().getValue() != false ? Math.signum(vertical) : vertical) * this.client.options.getMouseWheelSensitivity().getValue();
                double mouseX = this.x * (double) this.client.getWindow().getScaledWidth() / (double) this.client.getWindow().getWidth();
                double mouseY = this.y * (double) this.client.getWindow().getScaledHeight() / (double) this.client.getWindow().getHeight();
                hudOverlay.mouseScrolled(mouseX, mouseY, amount);
            }
        }
    }

    @Inject(method = "onCursorPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getProfiler()Lnet/minecraft/util/profiler/Profiler;", shift = At.Shift.AFTER, ordinal = 0))
    private void onMouseMove(long window, double x, double y, CallbackInfo ci) {
        if (this.client.getOverlay() instanceof HudOverlay hudOverlay) {
            double mouseX = x * (double) this.client.getWindow().getScaledWidth() / (double) this.client.getWindow().getWidth();
            double mouseY = y * (double) this.client.getWindow().getScaledHeight() / (double) this.client.getWindow().getHeight();
            HudOverlay.wrapScreenError(() -> hudOverlay.mouseMoved(mouseX, mouseY), "mouseMoved event handler", hudOverlay.getClass().getCanonicalName());
            if (this.activeButton != -1 && this.glfwTime > 0.0) {
                double deltaX = (x - this.x) * (double) this.client.getWindow().getScaledWidth() / (double) this.client.getWindow().getWidth();
                double deltaY = (y - this.y) * (double) this.client.getWindow().getScaledHeight() / (double) this.client.getWindow().getHeight();
                HudOverlay.wrapScreenError(() -> hudOverlay.mouseDragged(mouseX, mouseY, this.activeButton, deltaX, deltaY), "mouseDragged event handler", hudOverlay.getClass().getCanonicalName());
            }
        }
    }
}
