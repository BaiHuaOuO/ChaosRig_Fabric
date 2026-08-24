package dev.chaosrig.utils.renderer;

import dev.chaosrig.utils.ColorTools;
import dev.chaosrig.utils.KeyboardInput;
import dev.chaosrig.utils.KeyboardManager;
import dev.chaosrig.utils.Vec3dHelper;
import dev.chaosrig.utils.ping.PingRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CameraRenderer extends HudOverlay {
    protected final int initialFov;

    public CameraRenderer() {
        this.initialFov = MinecraftClient.getInstance().options.getFov().getValue();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer textRenderer = this.client.textRenderer;
        List<Text> texts = this.getControlTip();
        int maxTextWidth = 0;
        for (Text text : texts) {
            int textWidth = textRenderer.getWidth(text.getString());
            if (textWidth >= maxTextWidth) {
                maxTextWidth = textWidth;
            }
        }
        int width = this.client.getWindow().getScaledWidth();
        for (int i = 0; i < texts.size(); i++) {
            Text text = texts.get(i);
            context.drawText(textRenderer, text, width - maxTextWidth - 4,  i * textRenderer.fontHeight + 18, ColorTools.WHITE.apply(255), true);
        }
    }

    public List<Text> getControlTip() {
        List<Text> texts = new ArrayList<>();
        texts.add(Text.of(mouseControl ? "[RMB] Ping" : "[Z] Ping"));
        texts.add(Text.of(mouseControl ? "[SPACE](Release) Switch Control" : "[SPEAC](Holding) Switch Control"));
        return texts;
    }

    @Override
    public void onClose() {
        MinecraftClient.getInstance().options.getFov().setValue(this.initialFov);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyboardInput.PING.matchesKey(keyCode, scanCode)) {
            PingRenderer.getInstance().pingByCamera();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseControl && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            HitResult result = Vec3dHelper.ClientHelper.getScreenRaycast(128, false);
            if (result != null) {
                PingRenderer.getInstance().addByOthers(result);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseControl) {
            SimpleOption<Integer> fov = MinecraftClient.getInstance().options.getFov();
            fov.setValue((int) (fov.getValue() - amount));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseControl && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
                return false;
            }
            double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
            double distanceX = deltaX * scale;
            double distanceY = deltaY * scale;
            player.changeLookDirection(distanceX, distanceY);
            return true;
        }
        return false;
    }
}
