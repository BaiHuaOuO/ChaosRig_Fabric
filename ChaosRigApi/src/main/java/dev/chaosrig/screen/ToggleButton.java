package dev.chaosrig.screen;

import dev.chaosrig.utils.ColorTools;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Consumer;

public class ToggleButton extends BaseButton {
    public static final String ON_TEXT = "on";
    public static final String OFF_TEXT = "off";
    public static final String SEPARATOR_TEXT = " / ";
    public static final int ON_TEXT_WIDTH = MinecraftClient.getInstance().textRenderer.getWidth(ON_TEXT);
    public static final int OFF_TEXT_WIDTH = MinecraftClient.getInstance().textRenderer.getWidth(OFF_TEXT);
    public static final int SEPARATOR_TEXT_WIDTH = MinecraftClient.getInstance().textRenderer.getWidth(SEPARATOR_TEXT);
    public static final int TOGGLE_TEXT_WIDTH = ON_TEXT_WIDTH + OFF_TEXT_WIDTH + SEPARATOR_TEXT_WIDTH;
    protected boolean toggle;
    @Nullable
    protected Consumer<Boolean> toggleRun;

    public ToggleButton(int x, int y, int width, int height, boolean defaultToggle, @NotNull Text message, @Nullable Text description) {
        super(x, y, width, height, message, description);
        if (width < TOGGLE_TEXT_WIDTH + 10) {
            throw new IllegalArgumentException("width值必须大于%s".formatted(TOGGLE_TEXT_WIDTH));
        }
        this.toggle = defaultToggle;
    }

    public boolean isOn() {
        return this.toggle;
    }

    public void toggle() {
        if (!this.lock) {
            this.toggle = !this.toggle;
            if (this.toggleRun != null) {
                this.toggleRun.accept(this.toggle);
            }
        }
    }

    public void setToggleRunning(Consumer<Boolean> clickRun) {
        this.toggleRun = clickRun;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderButton(context, mouseX, mouseY, delta);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int endX = this.getX() + this.getWidth();
        int centerY = this.getY() + this.getHeight() / 2;
        int bgColor = lock ? ColorHelper.Argb.getArgb(255, 180, 180, 180) : ColorHelper.Argb.getArgb(255, 250, 250, 250);

        // render box
        context.drawText(textRenderer, "[", this.getX(), centerY, bgColor, false);
        context.drawText(textRenderer, "]", endX - TOGGLE_TEXT_WIDTH - 1, centerY, bgColor, false);

        // render text
        Text message = this.getMessage();
        int textWidth = textRenderer.getWidth(message);
        int maxTextWidth = this.getWidth() - TOGGLE_TEXT_WIDTH - 7;
        String text = textWidth <= maxTextWidth ? message.getString() : textRenderer.trimToWidth(message, maxTextWidth).getString() + "...";
        boolean hovering = this.hovered || this.isFocused();
        int textColor = lock
                ? hovering
                    ? ColorHelper.Argb.getArgb(255, 240, 240, 240)
                    : ColorHelper.Argb.getArgb(255, 150, 150, 150)
                : hovering
                    ? ColorHelper.Argb.getArgb(255, 240, 240, 240)
                    : ColorTools.GRAY.apply(255);
        context.drawText(textRenderer, text, this.getX() + 5, centerY, textColor, false);

        // render toggle
        context.drawText(textRenderer, toggle ? ON_TEXT.toUpperCase(Locale.ROOT) : ON_TEXT, endX - TOGGLE_TEXT_WIDTH + 5, centerY, this.toggle ? ColorTools.LIME.apply(255) : ColorTools.GRAY.apply(255), toggle);
        context.drawText(textRenderer, SEPARATOR_TEXT, endX - SEPARATOR_TEXT_WIDTH - OFF_TEXT_WIDTH + 3, centerY, ColorTools.WHITE.apply(255), false);
        context.drawText(textRenderer, toggle ? OFF_TEXT : OFF_TEXT.toUpperCase(Locale.ROOT), endX - OFF_TEXT_WIDTH, centerY, this.toggle ? ColorTools.GRAY.apply(255) : ColorTools.LIGHT_RED.apply(255), !toggle);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.toggle();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.lock) {
            return false;
        }
        if (!this.isNarratable()) {
            return false;
        }
        if (KeyCodes.isToggle(keyCode)) {
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            this.toggle();
            return true;
        }
        return false;
    }

    @Override
    protected Text getNarrationElementTitle() {
        return Text.of("Button: %s, Status: %s".formatted(this.getMessage().getString(), this.toggle ? "Enabled" : "Disabled"));
    }
}
