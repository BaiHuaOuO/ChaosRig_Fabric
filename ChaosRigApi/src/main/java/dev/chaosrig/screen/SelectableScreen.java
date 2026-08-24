package dev.chaosrig.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.chaosrig.utils.DescriptionRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public abstract class SelectableScreen extends Screen {
    @Nullable
    protected final Screen parent;
    protected int blockOffset = 1;
    protected int backgroundAlpha = 0;
    protected int blockColorOne = ColorHelper.Argb.getArgb(150, 255, 238, 87);
    protected int blockColorTwo = ColorHelper.Argb.getArgb(150, 255, 251, 209);
    protected boolean displayWarningBackground = false;
    @Nullable
    protected Element hoveringElement;
    protected short tick = 0;
    @Nullable
    protected DescriptionRenderer descriptionRenderer;
    protected int maxElementIndex = 0;
    protected ElementProvider elementProvider;
    protected int scrollOffset = 0;
    protected int maxScrollOffset = 0;
    protected double scrollSpeed = 0;

    public SelectableScreen(@NotNull Text title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.descriptionRenderer = new DescriptionRenderer(this.height - 28);
        this.elementProvider = new SimpleElementProvider(this, this.getElementStartX(), this.getElementStartY(), this.getElementWidth(), this.getElementHeight());
        this.initElement(this.elementProvider);
        this.elementProvider.result().forEach(this::addDrawableChild);
        this.elementProvider.clear();
        this.resetElementIndex();
        this.maxScrollOffset = Math.min(0, -this.maxElementIndex * this.getElementHeight() - MinecraftClient.getInstance().getWindow().getScaledHeight() - 50);
    }

    public int getElementStartX() {
        return 20;
    }

    public int getElementStartY() {
        return 20;
    }

    public int getElementWidth() {
        return this.width - this.width / 8;
    }

    public int getElementHeight() {
        return 16;
    }

    public int getScrollOffset() {
        return this.scrollOffset;
    }

    protected abstract void initElement(ElementProvider provider);

    @Override
    public void tick() {
        if (this.tick >= 21) {
            this.tick = 0;
        }
        this.tick++;
        for (Element element : this.children()) {
            if (element instanceof Tick tickInterface && this.tick % tickInterface.whenTickCall() == 0) {
                tickInterface.tick(this);
            }
        }
        if (this.descriptionRenderer != null) {
            this.descriptionRenderer.tick();
        }
        this.scrollOffset += (int) this.scrollSpeed;
        if (this.scrollOffset >= 0) {
            this.scrollOffset = 0;
        }
        if (this.scrollOffset <= this.maxScrollOffset) {
            this.scrollOffset = this.maxScrollOffset;
        }
    }

    public void resetElementIndex() {
        int index = 0;
        for (Element element : this.children()) {
            if (element instanceof BaseButton base && base.isNarratable()) {
                base.setIndex(index);
                index++;
            }
        }
        this.maxElementIndex = Math.max(0, index - 1);
    }

    public int getMaxElementIndex() {
        return maxElementIndex;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !(this.getFocused() instanceof LockEscExitScreen lock && lock.shouldLockEsc(this));
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    protected void updateRenderBackground(DrawContext context) {
        RenderBackground renderBackground = null;

        if (this.hoveringElement instanceof RenderBackground r) {
            renderBackground = r;
        } else if (this.getFocused() instanceof RenderBackground r) {
            renderBackground = r;
        }

        if (renderBackground != null && renderBackground.enableBackground(this)) {
            this.backgroundAlpha += 2;
            if (this.backgroundAlpha >= 254) {
                this.backgroundAlpha = 255;
            }
        } else {
            this.backgroundAlpha -= 2;
            if (this.backgroundAlpha <= 1) {
                this.backgroundAlpha = 0;
            }
        }

        this.displayWarningBackground = this.backgroundAlpha >= 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.enableBlend();
        this.renderBackground(context);
        if (this.descriptionRenderer != null) {
            this.descriptionRenderer.render(MinecraftClient.getInstance(), context);
        }
        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    @Override
    public void renderBackground(DrawContext context) {
        //context.fill(0, 0, this.width, this.height, -2, ColorTools.BLACK.apply(70)); // failed to mix
        MatrixStack matrices = context.getMatrices();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getRenderTypeGuiOverlayProgram);
        matrices.push();
        buffer.vertex(matrix, 0, 0, -2).color(0, 0, 0, 0.6f).next();
        buffer.vertex(matrix, 0, this.height, -2).color(0, 0, 0, 0.6f).next();
        buffer.vertex(matrix, this.width, this.height, -2).color(0, 0, 0, 0.6f).next();
        buffer.vertex(matrix, this.width, 0, -2).color(0, 0, 0, 0.6f).next();
        matrices.pop();
        Tessellator.getInstance().draw();
        updateRenderBackground(context);
        if (!this.displayWarningBackground) {
            return;
        }
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int height = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int blockSize = Math.max(width, height) / 9;
        int maxRow = height / blockSize + 2;
        int maxCol = width / blockSize + 2;

        this.blockOffset++;
        if (blockOffset > blockSize * 2) blockOffset = 1;
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getRenderTypeGuiOverlayProgram);
        matrices.push();
        for (int row = 0; row < maxRow; row++) {
            for (int col = 0; col < maxCol; col++) {
                int color = ((row + col) % 2 == 0) ? blockColorOne : blockColorTwo;
                int offset = (int) (blockSize / (blockSize * 2f) * blockOffset);
                int x1 = blockSize * col - blockSize + offset;
                int x2 = blockSize * col + offset;
                int y1 = blockSize * row - blockSize + offset;
                int y2 = blockSize * row + offset;
                float red = ColorHelper.Argb.getRed(color) / 255f;
                float green = ColorHelper.Argb.getGreen(color) / 255f;
                float blue = ColorHelper.Argb.getBlue(color) / 255f;
                int defaultAlpha = ColorHelper.Argb.getAlpha(color);
                float alpha = Math.min(defaultAlpha, backgroundAlpha * (defaultAlpha / 255f * 1.5f)) / 255f;
                buffer.vertex(matrix, x1, y1, -1).color(red, green, blue, alpha).next();
                buffer.vertex(matrix, x1, y2, -1).color(red, green, blue, alpha).next();
                buffer.vertex(matrix, x2, y2, -1).color(red, green, blue, alpha).next();
                buffer.vertex(matrix, x2, y1, -1).color(red, green, blue, alpha).next();
            }
        }
        matrices.pop();
        Tessellator.getInstance().draw();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.getFocused() != null && !this.getFocused().isMouseOver(mouseX, mouseY)) {
            this.setFocused(null);
        }
        for (Element element : this.children()) {
            if (element.isMouseOver(mouseX, mouseY)) {
                this.hoveringElement = element;
                return;
            }
        }
        this.hoveringElement = null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.scrollSpeed += amount * 5.5;
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    public interface LockEscExitScreen {

        boolean shouldLockEsc(SelectableScreen instance);
    }

    public interface Tick {

        void tick(SelectableScreen instance);

        short whenTickCall();
    }

    public interface Locker<T> {

        @NotNull
        T getElementInstance();

        boolean isLock();

        void lock(boolean value);

        default void lock(@NotNull Function<T, Boolean> checker) {
            this.lock(checker.apply(this.getElementInstance()));
        }

        void setWaringMessage(@NotNull Text text);
    }

    public interface RenderBackground {
        boolean enableBackground(SelectableScreen instance);
    }
}
