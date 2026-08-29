package dev.chaosrig.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class DescriptionRenderer {
    @Nullable
    protected Supplier<Text> description;
    protected boolean lock = false;
    protected int textStep = 0;
    protected final int atY;

    public DescriptionRenderer(int y) {
        this.atY = y;
    }

    public void render(MinecraftClient client, DrawContext context) {
        if (this.description == null) {
            return;
        }
        client.getProfiler().push("preparing render description");
        TextRenderer fontRenderer = client.textRenderer;
        String renderText = this.description.get().asTruncatedString(textStep);
        Window window = client.getWindow();
        int maxWidth = window.getScaledWidth() - 20;
        List<OrderedText> textList = fontRenderer.wrapLines(StringVisitable.plain(renderText), maxWidth);
        if (textList.isEmpty()) {
            return;
        }
        int textHeight = textList.size() * fontRenderer.fontHeight;
        float scale = Math.max(0, (float) (fontRenderer.fontHeight - textList.size() + 1) / fontRenderer.fontHeight);
        int x = window.getScaledWidth() / 2;
        client.getProfiler().push("rendering description");
        for (OrderedText text : textList) {
            int textWidth = fontRenderer.getWidth(text);
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 1.0f);
            context.drawText(fontRenderer,
                    text,
                    x - (textWidth / 2),
                    this.atY - textHeight,
                    0xFFFFFFFF,
                    true);
            context.getMatrices().pop();
        }
        client.getProfiler().pop();
    }

    public void tick() {
        if (this.description == null) {
            return;
        }
        if (description.get().getString().length() > textStep) {
            this.textStep++;
        }
    }

    public void push(@NotNull Supplier<Text> description) {
        if (this.lock) {
            return;
        }
        if (description.equals(this.description)) {
            return;
        }
        this.resetText(description);
    }

    public void clear() {
        this.description = null;
        this.textStep = 0;
    }

    protected void resetText(@NotNull Supplier<Text> description) {
        this.description = description;
        this.textStep = 0;
    }

}
