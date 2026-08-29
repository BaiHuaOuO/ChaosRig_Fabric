package dev.chaosrig;

import dev.chaosrig.gamefunc.StayTogetherRenderer;
import dev.chaosrig.utils.KeyboardManager;
import dev.chaosrig.utils.TooltipReader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ChaosRigClient implements ClientModInitializer {
    private static boolean init = false;

    @Override
    public void onInitializeClient() {
        KeyboardManager.register();
        StayTogetherRenderer.register();
        TooltipReader.register();
        init = true;
    }

    public static boolean isInit() {
        return init;
    }
}
