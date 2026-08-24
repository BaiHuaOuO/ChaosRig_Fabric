package dev.chaosrig.utils;

import dev.chaosrig.ChaosRigClient;
import dev.chaosrig.utils.ping.PingRenderer;
import dev.chaosrig.utils.renderer.CameraRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyboardManager {

    public static void register() {
        if (ChaosRigClient.isInit()) throw new RuntimeException("重复注册");

    }
}
