package dev.chaosrig.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class BaseButton extends ClickableWidget implements SelectableScreen.RenderBackground, SelectableScreen.Locker<BaseButton>, SelectableScreen.Tick {
    protected boolean lock = false;
    @Nullable
    protected Text waringMessage;
    protected Text description;
    protected int index = 0;
    @Nullable
    protected Runnable elementIndexCallback;
    @Nullable
    protected Supplier<Integer> scrollOffsetCallback;

    public BaseButton(int x, int y, int width, int height, @NotNull Text message, @Nullable Text description) {
        super(x, y, width, height, message);
        this.description = description;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setElementIndexCallback(@NotNull Runnable elementIndexCallback) {
        this.elementIndexCallback = elementIndexCallback;
    }

    public void setScrollOffsetCallback(@Nullable Supplier<Integer> scrollOffsetCallback) {
        this.scrollOffsetCallback = scrollOffsetCallback;
    }

    @Override
    public boolean enableBackground(SelectableScreen instance) {
        return this.waringMessage != null && (this.hovered || this.isFocused());
    }

    @Override
    public int getY() {
        return super.getY() + this.index * this.height + (this.scrollOffsetCallback == null ? 0 : this.scrollOffsetCallback.get());
    }

    @Override
    public @NotNull BaseButton getElementInstance() {
        return this;
    }

    @Override
    public boolean isLock() {
        return this.lock;
    }

    @Override
    public void lock(boolean value) {
        this.lock = value;
    }

    @Override
    public void setWaringMessage(@NotNull Text text) {
        this.waringMessage = text;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        // render tooltip [warning status]
        if (this.waringMessage != null) {
            if (this.hovered) {
                context.drawTooltip(textRenderer, this.waringMessage, mouseX, mouseY);
            } else if (this.isFocused()) {
                context.drawTooltip(textRenderer, this.waringMessage, this.getX() + this.getWidth() + 45, this.getY() + this.getHeight() + 45);
            }
        }
    }

    @Override
    public void tick(SelectableScreen instance) {
        if (this.hovered || this.isFocused()) {
            if (this.description != null && instance.descriptionRenderer != null) {
                instance.descriptionRenderer.push(lock ? Text.of(this.description.getString() + " (已锁定)") : this.description);
            }
        }
    }

    public void setNarratable(boolean value) {
        this.visible = value;
    }

    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public short whenTickCall() {
        return 5;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.getNarrationElementTitle());
        if (this.description != null) {
            builder.put(NarrationPart.USAGE, this.description);
        }
        if (this.lock) {
            builder.put(NarrationPart.HINT, "该元素已被锁定");
        }
        if (this.waringMessage != null) {
            builder.put(NarrationPart.HINT, this.waringMessage);
        }
    }

    protected abstract Text getNarrationElementTitle();
}
