package dev.chaosrig.screen;

import dev.chaosrig.utils.ColorTools;
import dev.chaosrig.utils.renderer.InformationScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class SliderButton extends BaseButton implements SelectableScreen.LockEscExitScreen {
    protected double value;
    protected double minValue;
    protected double maxValue;
    protected boolean sliderFocused = false;
    protected boolean editBoxFocused = false;
    protected boolean dragSlider = false;
    @Nullable
    protected Consumer<Double> valueChangeRun;
    @NotNull
    protected final EditBox editBox;
    protected short renderCursorStep = 5;

    public SliderButton(int x, int y, int width, int height, double defaultValue, @NotNull Text message, @Nullable Text description) {
        super(x, y, width, height, message, description);
        this.value = defaultValue;
        if (value < minValue) {
            this.minValue = value;
        }
        if (value > maxValue) {
            this.maxValue = value;
        }
        this.editBox = new EditBox(MinecraftClient.getInstance().textRenderer, this.width);
        this.editBox.setText(String.valueOf(defaultValue));
        this.setMinValue(0);
        this.setMaxValue(1);
        InformationScreen.push("editBoxText", -1, ColorTools.WHITE.apply(255), this.editBox::getText);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderButton(context, mouseX, mouseY, delta);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int centerY = this.getY() + this.getHeight() / 2;
        int bgColor = lock ? ColorHelper.Argb.getArgb(255, 180, 180, 180) : ColorHelper.Argb.getArgb(255, 250, 250, 250);
        boolean focusing = this.hovered || this.isFocused();

        // render box
        context.drawText(textRenderer, "[", this.getX(), centerY, bgColor, false);
        context.drawText(textRenderer, "]", this.getSliderEndX() + 2, centerY, bgColor, false);

        // render message
        Text message = this.getMessage();
        int textWidth = textRenderer.getWidth(message);
        int maxTextWidth = this.getClickSliderWidth() - 4;
        String text = textWidth <= maxTextWidth ? message.getString() : textRenderer.trimToWidth(message, maxTextWidth).getString() + "...";
        int messageColor = lock
                ? ColorHelper.Argb.getArgb(focusing ? 90 : 255, 150, 150, 150)
                : focusing
                    ? ColorHelper.Argb.getArgb(90, 240, 240, 240)
                    : ColorTools.WHITE.apply(255);
        context.drawText(textRenderer, text, this.getX() + 5, centerY, messageColor, false);

        // render slider
        double progress = (this.maxValue == this.minValue) ? 0.0 : (this.value - this.minValue) / (this.maxValue - this.minValue);
        int renderOffset = MathHelper.clamp((int) (this.getSliderStartX() + this.getClickSliderWidth() * progress), this.getSliderStartX(), this.getSliderEndX());
        int renderAlpha = focusing ? 255 : 90;
        context.fill(this.getSliderStartX(), centerY + 4, renderOffset, centerY + 5, ColorTools.WHITE.apply(renderAlpha));
        context.fill(renderOffset, centerY + 4, this.getSliderEndX(), centerY + 5, ColorTools.GRAY.apply(renderAlpha));
        context.fill(renderOffset - 1, centerY, renderOffset + 1, centerY + 7, focusing ? ColorTools.WHITE.apply(255) : ColorHelper.Argb.getArgb(90, 180, 180, 180));

        // render value
        int textX = this.getX() + this.getSliderWidth() + 5;
        String valueText = this.editBoxFocused ? this.editBox.getText() : String.format(java.util.Locale.ROOT, "%.2f", this.value);
        int valueColor = lock
                ? ColorHelper.Argb.getArgb(255, 150, 150, 150)
                : ColorHelper.Argb.getArgb(255, 240, 240, 240);
        context.drawText(textRenderer, valueText, textX, centerY, valueColor, false);
        // render value which is mouse at (only mouse)
        if (!this.lock && !this.dragSlider && this.hovered && this.inSlider(mouseX)) {
            double newValue = this.getValueFromMouse(mouseX);
            String newValueText = "->" + String.format(java.util.Locale.ROOT, "%.2f", newValue);
            context.drawText(textRenderer, newValueText, textX + textRenderer.getWidth(valueText) + 1, centerY, ColorTools.LIME.apply(255), false);
        }
        // render editBox cursor
        if (this.editBoxFocused) {
            boolean selecting = this.editBox.hasSelection();
            if (!selecting && this.renderCursorStep >= 2) {
                int valueWidth = textRenderer.getWidth(valueText);
                int charWidth = textRenderer.getWidth(this.editBox.getText().substring(this.editBox.getCursor()));
                context.fill(textX + valueWidth - charWidth, centerY - 2, textX + valueWidth - charWidth + 1, centerY + 9, ColorTools.WHITE.apply(255));
            }
            if (selecting) {
                int cursorBegin = this.editBox.getSelection().beginIndex();
                int cursorEnd = this.editBox.getSelection().endIndex();
                int start = Math.min(cursorBegin, cursorEnd);
                int end = Math.max(cursorBegin, cursorEnd);
                int unselectWidth = textRenderer.getWidth(this.editBox.getText().substring(0, start));
                int selectTextWidth = textRenderer.getWidth(this.editBox.getText().substring(start, end));
                context.fill(textX + unselectWidth, centerY - 2, textX + unselectWidth + selectTextWidth, centerY + 9, ColorTools.GRAY.apply(130));
            }
        }
    }

    @Override
    public void tick(SelectableScreen instance) {
        super.tick(instance);
        renderCursorStep++;
        if (this.renderCursorStep >= 4) {
            this.renderCursorStep = 0;
        }
    }

    protected int getSliderStartX() {
        return this.getX() + 4;
    }

    protected int getSliderEndX() {
        return this.getX() + this.getSliderWidth() - 4;
    }

    protected int getSliderWidth() {
        return (int) (this.width - (this.width / 3d));
    }

    protected int getClickSliderWidth() {
        return Math.max(0, this.getSliderEndX() - this.getSliderStartX());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.lock) {
            return;
        }
        if (this.inInputBox(mouseX)) {
            this.sliderFocused = false;
            this.editBoxFocused = true;
            this.editBox.setSelecting(SelectableScreen.hasShiftDown());
            this.editBoxMouseCursor(mouseX, mouseY);
        }
        if (this.inSlider(mouseX)) {
            this.editBoxFocused = false;
            this.sliderFocused = true;
            this.setValueFromMouse(mouseX);
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.lock) {
            return;
        }
        if (this.editBoxFocused && this.inInputBox(mouseX)) {
            this.editBox.setSelecting(true);
            this.editBoxMouseCursor(mouseX, mouseY);
            this.editBox.setSelecting(SelectableScreen.hasShiftDown());
        }
        if (this.sliderFocused && this.inSlider(mouseX)) {
            this.dragSlider = true;
            this.setValueFromMouse(mouseX);
        } else {
            this.dragSlider = false;
        }
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.dragSlider = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.lock) {
            return false;
        }
        if (!this.isNarratable()) {
            return false;
        }
        if (this.editBoxFocused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.editBoxFocused = false;
                this.setValueFromText();
                return true;
            }
            this.editBox.handleSpecialKey(keyCode);
            return true;
        }
        if (this.sliderFocused) {
            boolean leftInput = keyCode == GLFW.GLFW_KEY_LEFT;
            if (leftInput || keyCode == GLFW.GLFW_KEY_RIGHT) {
                float offsetValue = leftInput ? -1.0f : 1.0f;
                double step = (this.maxValue - this.minValue) / this.getSliderWidth();
                this.setValue(this.value + offsetValue / step);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.lock) {
            return false;
        }
        if (!this.isNarratable()) {
            return false;
        }
        if (!SharedConstants.isValidChar(chr)) {
            return false;
        }
        if (this.editBoxFocused) {
            this.editBox.replaceSelection(Character.toString(chr));
            return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.sliderFocused = false;
            return;
        }
        GuiNavigationType guiNavigationType = MinecraftClient.getInstance().getNavigationType();
        if (guiNavigationType == GuiNavigationType.MOUSE || guiNavigationType == GuiNavigationType.KEYBOARD_TAB) {
            this.sliderFocused = true;
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.editBoxFocused || super.isMouseOver(mouseX, mouseY);
    }

    public void setValueChangeRun(@Nullable Consumer<Double> valueChangeRun) {
        this.valueChangeRun = valueChangeRun;
    }

    public boolean inSlider(double mouseX) {
        return this.getSliderStartX() <= mouseX && this.getSliderEndX() >= mouseX;
    }

    public boolean inInputBox(double mouseX) {
        return this.getSliderEndX() <= mouseX && this.getX() + this.getWidth() >= mouseX;
    }

    protected void editBoxMouseCursor(double mouseX, double mouseY) {
        double x = mouseX - this.getX() - 4;
        double y = mouseY - this.getY() - 1;
        this.editBox.moveCursor(x, y);
    }

    public void setValue(double value) {
        if (this.lock) {
            return;
        }
        if (this.value != value) {
            this.value = MathHelper.clamp(value, this.minValue, this.maxValue);
            this.editBox.setText(String.valueOf(this.value));
            this.applyValue();
        }
        this.callbackSetValue();
    }

    public void setValueFromText() {
        try {
            StringBuilder sb = new StringBuilder();
            for (char c : this.editBox.getText().toCharArray()) {
                if (!SharedConstants.isValidChar(c)) {
                    continue;
                }
                sb.append(c);
            }
            double value = Double.parseDouble(sb.toString());
            this.setValue(value);
        } catch(NumberFormatException e) {
            this.editBox.setText(String.valueOf(this.value));
            // TODO: 不合法提示
        }
    }

    public void setValueFromMouse(double mouseX) {
        this.setValue(this.getValueFromMouse(mouseX));
    }

    public double getValueFromMouse(double mouseX) {
        double ratio = MathHelper.clamp((mouseX - this.getSliderStartX()) / this.getClickSliderWidth(), 0d, 1d);
        return this.minValue + ratio * (this.maxValue - this.minValue);
    }

    protected void callbackSetValue() {
    }

    protected void applyValue() {
        if (this.valueChangeRun != null) {
            this.valueChangeRun.accept(this.value);
        }
    }

    public void setMaxValue(double maxValue) {
        if (this.maxValue < minValue) {
            maxValue = this.minValue;
        }
        this.maxValue = maxValue;
        this.editBox.setMaxLength(String.format(java.util.Locale.ROOT, "%.2f", maxValue).length() + 3);
    }

    public void setMinValue(double minValue) {
        if (this.minValue > this.maxValue) {
            minValue = this.maxValue;
        }
        this.minValue = minValue;
    }

    public double getValue() {
        return value;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    @Override
    protected Text getNarrationElementTitle() {
        return Text.of("Slider: %s, Value: %s(Min: %s, Max: %s)".formatted(this.getMessage().getString(),
                String.format(java.util.Locale.ROOT, "%.2f", this.value),
                String.format(java.util.Locale.ROOT, "%.2f", this.minValue),
                String.format(java.util.Locale.ROOT, "%.2f", this.maxValue)));
    }

    @Override
    public boolean shouldLockEsc(SelectableScreen instance) {
        return this.editBoxFocused;
    }
}
