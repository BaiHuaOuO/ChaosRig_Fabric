package dev.chaosrig.utils.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.util.Window;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public abstract class HudOverlay extends Overlay implements Element {
    protected boolean mouseControl = false;
    public final MinecraftClient client = MinecraftClient.getInstance();

    public HudOverlay() {
    }

    public static void wrapScreenError(Runnable task, String errorTitle, String screenName) {
        try {
            task.run();
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.create(throwable, errorTitle);
            CrashReportSection crashReportSection = crashReport.addElement("Affected overlay");
            crashReportSection.add("Overlay name", () -> screenName);
            throw new CrashException(crashReport);
        }
    }

    /**
     * <p>关闭{@link Overlay}显示</p>
     * @see HudOverlay#onClose()
     * @see HudOverlay#shouldClose()
     */
    public void close() {
        if (this.shouldClose()) {
            this.onClose();
            MinecraftClient.getInstance().setOverlay(null);
        }
    }

    public void onClose() {}

    /**
     * <p>是否满足条件关闭当前{@link Overlay}</p>
     * @return 是与否
     * @see HudOverlay#close()
     */
    public boolean shouldClose() {
        return true;
    }


    /**
     * <p>当有按键被按下时该方法会被调用, <code>keyCode</code>与{@link GLFW}内按键代码等价</p>
     * <p>⚠该方法的返回值会决定是否取消原版逻辑</p>
     * @param keyCode {@link org.lwjgl.glfw.GLFW GLFW}类所定义的按键代码
     * @param scanCode 按键唯一特征码
     * @param modifiers 按住键的GLFW字段(请参考<a href="https://www.glfw.org/docs/3.3/group__mods.html">GLFW Modifier key flags</a>)
     * @return <code>true</code>取消原版逻辑, 反之不取消
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            this.setMouseControl(true);
        }
        return true;
    }

    /**
     * <p>当有按键松开时该方法会被调用, <code>keyCode</code>与{@link GLFW}内按键代码等价</p>
     * <p>⚠该方法的返回值会决定是否取消原版逻辑</p>
     * @param keyCode {@link org.lwjgl.glfw.GLFW GLFW}类所定义的按键代码
     * @param scanCode 按键唯一特征码
     * @param modifiers 按住键的GLFW字段(请参考<a href="https://www.glfw.org/docs/3.3/group__mods.html">GLFW Modifier key flags</a>)
     * @return <code>true</code>取消原版逻辑, 反之不取消
     */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            this.setMouseControl(false);
        }
        return true;
    }

    /**
     * <p>当任意字符输入时该方法会被调用</p>
     * @param chr 输入的字符
     * @param modifiers 按住键的GLFW字段(请参考<a href="https://www.glfw.org/docs/3.3/group__mods.html">GLFW Modifier key flags</a>)
     * @return <code>true</code>事件成功处理, 反之失败
     */
    @Override
    public boolean charTyped(char chr, int modifiers) {
        return Element.super.charTyped(chr, modifiers);
    }

    /**
     * <p>鼠标移动时该方法会被调用</p>
     * <p>以(0, 0)[窗口左上角]为原点</p>
     * @param mouseX 相对于窗口, 鼠标横坐标(已缩放)
     * @param mouseY 相对于窗口, 鼠标纵坐标(已缩放)
     * @see Window#getScaleFactor() 窗口缩放因子
     */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        Element.super.mouseMoved(mouseX, mouseY);
    }

    /**
     * <p>当鼠标按键按下时该方法会被调用</p>
     * <p>以(0, 0)[窗口左上角]为原点</p>
     * @param mouseX 相对于窗口, 鼠标横坐标(已缩放)
     * @param mouseY 相对于窗口, 鼠标纵坐标(已缩放)
     * @param button 鼠标按键的{@link GLFW}按键代码
     * @see Window#getScaleFactor() 窗口缩放因子
     * @return <code>true</code>事件成功处理, 反之失败
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return Element.super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * <p>当鼠标按键松开时该方法会被调用</p>
     * <p>以(0, 0)[窗口左上角]为原点</p>
     * @param mouseX 相对于窗口, 鼠标横坐标(已缩放)
     * @param mouseY 相对于窗口, 鼠标纵坐标(已缩放)
     * @param button 鼠标按键的{@link GLFW}按键代码
     * @see Window#getScaleFactor() 窗口缩放因子
     * @return <code>true</code>事件成功处理, 反之失败
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return Element.super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * <p>当鼠标拖拽时该方法会被调用</p>
     * <p>以(0, 0)[窗口左上角]为原点</p>
     * @param mouseX 相对于窗口, 当前鼠标横坐标(已缩放)
     * @param mouseY 相对于窗口, 当前鼠标纵坐标(已缩放)
     * @param button 鼠标按键的{@link GLFW}按键代码
     * @param deltaX 相对于窗口, 之前鼠标横坐标与当前鼠标横坐标的差值
     * @param deltaY 相对于窗口, 之前鼠标纵坐标与当前鼠标纵坐标的差值
     * @see Window#getScaleFactor() 窗口缩放因子
     * @return <code>true</code>事件成功处理, 反之失败
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return Element.super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    /**
     * <p>当鼠标滚轮滑动时该方法会被调用</p>
     * <p>以(0, 0)[窗口左上角]为原点</p>
     * @param mouseX 相对于窗口, 鼠标横坐标(已缩放)
     * @param mouseY 相对于窗口, 鼠标纵坐标(已缩放)
     * @param amount 值<code>小于</code>0为下滑, <code>大于</code>0为上滑
     * @see Window#getScaleFactor() 窗口缩放因子
     * @return <code>true</code>事件成功处理, 反之失败
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return Element.super.mouseScrolled(mouseX, mouseY, amount);
    }

    /**
     * <p>由于{@link HudOverlay}的特殊性, 该方法可以调控控制逻辑</p>
     * @param value 是否为鼠标控制模式
     */
    public void setMouseControl(boolean value) {
        this.mouseControl = value;
        if (value) {
            MinecraftClient.getInstance().mouse.unlockCursor();
            this.onUnlockCursor();
        } else {
            MinecraftClient.getInstance().mouse.lockCursor();
            this.onLockCursor();
        }
    }

    protected void onUnlockCursor() {}

    protected void onLockCursor() {}

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return true;
    }

    @Override
    public boolean pausesGame() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() {
        return true;
    }
}
