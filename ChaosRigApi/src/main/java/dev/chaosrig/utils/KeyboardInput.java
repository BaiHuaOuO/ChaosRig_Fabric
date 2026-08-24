package dev.chaosrig.utils;

import dev.chaosrig.ChaosRigApiClient;
import dev.chaosrig.screen.ConfigScreen;
import dev.chaosrig.screen.SelectableScreen;
import dev.chaosrig.screen.SliderButton;
import dev.chaosrig.screen.ToggleButton;
import dev.chaosrig.utils.ping.PingRenderer;
import dev.chaosrig.utils.renderer.PostShaders;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeyboardInput {
    public final static KeyBinding PING = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.chaosrig.ping",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "ChaosRig"));
    public final static KeyBinding TEST = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.chaosrig.test",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "ChaosRig"));

    public static void register() {
        if (ChaosRigApiClient.isInit()) throw new RuntimeException("重复注册");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (PING.wasPressed()) {
                PingRenderer.getInstance().ping();
            }
            while (TEST.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new ConfigScreen(Text.of("Test"), null));
            }
        });
    }
}
