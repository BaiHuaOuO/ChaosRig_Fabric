package dev.chaosrig.utils.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.ActionResult;

@Environment(EnvType.CLIENT)
public class GameRendererEvent {
    public static final Event<RenderEvent> TAIL = EventFactory.createArrayBacked(RenderEvent.class,
            (listeners) -> (gameRenderer, client, drawContext, tickDelta, startTime, tick) -> {
                for (RenderEvent listener : listeners) {
                    ActionResult result = listener.onRender(gameRenderer, client, drawContext, tickDelta, startTime, tick);
                    if (result != ActionResult.PASS) return result;
                }
                return ActionResult.PASS;
            });

    @FunctionalInterface
    public interface RenderEvent {
        ActionResult onRender(GameRenderer gameRenderer, MinecraftClient client, DrawContext drawContext, float tickDelta, long startTime, boolean tick);
    }
}
