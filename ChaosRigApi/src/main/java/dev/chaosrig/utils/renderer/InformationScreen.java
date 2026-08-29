package dev.chaosrig.utils.renderer;

import dev.chaosrig.ChaosRigApiClient;
import dev.chaosrig.utils.ColorTools;
import dev.chaosrig.utils.event.GameRendererEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>客户端屏幕上渲染信息</p>
 * <p>
 *     文本渲染顺序根据添加先后顺序, 并没有绝对的置于顶层与置于底层, 这在有些时候是不方便的 <br>
 *     文本宽度超过窗口宽度会自动换行, 文本数量超过窗口高度会添加页数. <br>
 *     页数切换与切换是否显示仅接受客户端键盘中的:
 *     <ul>
 *         <li><code>←</code>上一页</li>
 *         <li><code>→</code>下一页</li>
 *         <li><code>F3</code>显示/隐藏</li>
 *     </ul>
 *     或是手动调用{@link InformationScreen}内部提供方法
 * </p>
 * <p>
 *     如果在客户端还未加载纹理之前添加信息, 会导致显示"□□□□" <br>
 *     仅加载完纹理后才会正常显示字体.
 * </p>
 * <p>
 *     文本接受<code>4</code>个参数, 详见{@link InformationScreen#push(String, int, int, Supplier)}或{@link Message} <br>
 *     文本标识符重复推送会直接<code>替换</code>列表内已存在的{@link Message}
 *     文本取消显示通过<code>context</code>来取消
 * </p>
 * <p>
 *     当{@link Message#messageSupplier}不为<code>null</code>, 显示内容为: <code>Context: {supplier.get()}</code>
 *     当{@link Message#messageSupplier}为<code>null</code>, 显示内容为: <code>Context</code>
 * </p>
 * @see InformationScreen#previousPage()
 * @see InformationScreen#nextPage()
 * @see InformationScreen#toggleClose()
 */
@Environment(EnvType.CLIENT)
public class InformationScreen {
    public static final Event<KeyboardEvent> KEYBOARD_EVENT = EventFactory.createArrayBacked(KeyboardEvent.class, keyboardEvents -> (window, key, scancode, action, modifiers) -> {
        for (KeyboardEvent event : keyboardEvents) {
            event.onInput(window, key, scancode, action, modifiers);
        }
    });
    protected static final ArrayList<Message> messages = new ArrayList<>();
    /**
     * 页面位置
     */
    protected static int page = 0;
    public static final int LINE_SPACE = 9;
    private static final Function<Integer, Integer> indexToYOffset = index -> index * LINE_SPACE;
    /**
     * 若已关闭(<code>true</code>), 则屏幕不会显示内容, 但正常接受{@link InformationScreen#push(String, int, int, Supplier)}&{@link InformationScreen#pop(String)}
     */
    protected static boolean closed = false;

    public static void register() {
        if (ChaosRigApiClient.isInit()) throw new RuntimeException("不允许重复注册");
        GameRendererEvent.TAIL.register(InformationScreen::onRender);
        ClientTickEvents.END_CLIENT_TICK.register(InformationScreen::onUpdate);
        KEYBOARD_EVENT.register((window, key, scancode, action, modifiers) -> {
            if (!closed) {
                if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_LEFT)) {
                    InformationScreen.previousPage();
                }
                if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_RIGHT)) {
                    InformationScreen.nextPage();
                }
            }
            if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_F3)) {
                InformationScreen.toggleClose();
            }
        });
    }

    protected static void onUpdate(MinecraftClient client) {
        if (closed) return;
        if (messages.isEmpty()) return;
        Iterator<Message> messagesIterator = messages.iterator();
        while (messagesIterator.hasNext()) {
            Message message = messagesIterator.next();
            message.tick();
            if (message.isCanceled()) messagesIterator.remove();
        }
    }

    protected static ActionResult onRender(GameRenderer gameRenderer, MinecraftClient client, DrawContext drawContext, float tickDelta, long startTime, boolean tick) {
        if (closed) return ActionResult.PASS;
        if (messages.isEmpty()) return ActionResult.PASS;
        ArrayList<Message> copy = new ArrayList<>(messages);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int line = 0;
        for (int i = contextIndex(); i < maxIndex() + 1; i++) {
            if (i >= copy.size()) break;
            Message message = copy.get(i);
            List<OrderedText> infos = textRenderer.wrapLines(StringVisitable.plain(message.getMessage()), MinecraftClient.getInstance().getWindow().getScaledWidth() - 20);
            for (OrderedText info : infos) {
                drawText(drawContext,
                        info,
                        0,
                        indexToYOffset.apply(line),
                        message.color);
                line++;
            }
        }
        int maxPage = getMaxPage();
        int page = getPage();
        boolean hadPage = maxPage > 0;
        if (hadPage) {
            drawText(drawContext,
                    "[Page: %s/%s]".formatted(page, maxPage),
                    1,
                    indexToYOffset.apply(line) + 10,
                    ColorTools.WHITE.apply(255));
            line++;
        }
        drawText(drawContext,
                "[F3] Hide/Show" + ((hadPage) ? " | [←] Previous | [→] Next" : ""),
                1,
                indexToYOffset.apply(line) + 10,
                ColorTools.WHITE.apply(255));
        return ActionResult.PASS;
    }

    private static void drawText(DrawContext drawContext, String text, int x, int y, int color) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        drawContext.fill(x, y, x + textRenderer.getWidth(text), y + textRenderer.fontHeight, ColorTools.getColor(160, 220, 220, 220));
        drawContext.drawText(textRenderer, text, x, y, color, true);
    }

    private static void drawText(DrawContext drawContext, OrderedText text, int x, int y, int color) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        drawContext.fill(x, y, x + textRenderer.getWidth(text), y + textRenderer.fontHeight, ColorTools.getColor(160, 220, 220, 220));
        drawContext.drawText(textRenderer, text, x, y, color, true);
    }

    /**
     * 切换关闭状态
     * @see InformationScreen#closed
     */
    public static void toggleClose() {
        closed = !closed;
    }

    /**
     * 是否为关闭状态
     * @return 是与否
     * @see InformationScreen#closed
     */
    public static boolean isClosed() {
        return closed;
    }

    /**
     * 上一页
     */
    public static void previousPage() {
        if (page > 0) page--;
    }

    /**
     * 下一页
     */
    public static void nextPage() {
        if (page < getMaxPage()) page++;
    }

    /**
     * 当前页数
     * @return 区间: [0, Int.max]
     */
    public static int getPage() {
        return page;
    }

    /**
     * 最大索引处
     */
    protected static int maxIndex() {
        return Math.min(contextIndex() + getMaxContextLine(), messages.size() - 1);
    }

    /**
     * 当前索引开始处
     */
    protected static int contextIndex() {
        return Math.max(0, getMaxContextLine() * page);
    }

    /**
     * 最大页数
     * @return 区间: [0, Int.max]
     */
    public static int getMaxPage() {
        return (messages.isEmpty()) ? 0 : (int) Math.ceil((double) messages.size() / getMaxContextLine()) - 1;
    }

    /**
     * 当前窗口大小最多容纳的行数
     * @return 区间: (0, Int.max]
     */
    public static int getMaxContextLine() {
        return Math.max(1, (MinecraftClient.getInstance().getWindow().getScaledHeight() - 70) / LINE_SPACE);
    }

    /**
     * 推送一个信息
     * @param context 上下文内容, 也作为取消信息的依据
     * @param maxTick 显示的最长时间, 若传参<code>maxTick < 0</code>, 则无最大显示时间
     * @param color 文本颜色
     * @param contextMessage 上下文需要渲染的内容信息
     * @return 信息实例
     * @see InformationScreen#pop(String)
     */
    @NotNull
    public static Message push(@NotNull String context, int maxTick, int color, @Nullable Supplier<String> contextMessage) {
        messages.removeIf(copy -> copy.context.equals(context));
        Message message = new Message(context, contextMessage, maxTick, color);
        messages.add(message);
        return message;
    }

    /**
     * 取消一个信息
     * @param context 通过{@link Message#context}来匹配对应的信息
     * @return <code>true</code>取消成功, 反之<code>false</code>
     */
    public static boolean pop(@NotNull String context) {
        for (Message message : messages) {
            if (message.context.equals(context)) {
                message.cancel();
            }
        }
        return false;
    }

    /**
     * 取消全部信息的显示
     */
    public static void popAll() {
        for (Message message : messages) {
            message.cancel();
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Message {
        @NotNull
        public final String context;
        @Nullable
        protected final Supplier<String> messageSupplier;
        private long tick = 0;
        protected boolean cancel = false;
        public final long maxTick;
        public final int color;

        public Message(@NotNull String context, @Nullable Supplier<String> messageSupplier, int maxTick, int color) {
            this.context = context;
            this.messageSupplier = messageSupplier;
            if (maxTick < 1) {
                maxTick = -1;
            }
            this.maxTick = maxTick;
            this.color = color;
        }

        public void cancel() {
            this.cancel = true;
        }

        public boolean isCanceled() {
            return this.cancel;
        }

        public final void tick() {
            if (this.cancel) return;
            tick++;
            if (this.maxTick < this.tick && this.maxTick != -1) {
                this.cancel = true;
            }
            this.onTick();
        }

        protected void onTick() {}

        public String getMessage() {
            if (messageSupplier != null) {
                return context + ':' + ' ' + messageSupplier.get();
            }
            return context;
        }

        public long getTick() {
            return this.tick;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Message message)) return false;
            return cancel == message.cancel && maxTick == message.maxTick && context.equals(message.context) && Objects.equals(messageSupplier, message.messageSupplier);
        }

        @Override
        public int hashCode() {
            int result = context.hashCode();
            result = 31 * result + Objects.hashCode(messageSupplier);
            result = 31 * result + Boolean.hashCode(cancel);
            result = 31 * result + Long.hashCode(maxTick);
            return result;
        }
    }

    public interface KeyboardEvent {
        void onInput(long window, int key, int scancode, int action, int modifiers);
    }
}
