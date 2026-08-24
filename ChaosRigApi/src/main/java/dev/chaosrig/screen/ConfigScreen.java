package dev.chaosrig.screen;

import dev.chaosrig.utils.config.ClientChaosRigApiConfig;
import dev.chaosrig.utils.renderer.PostShaders;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ConfigScreen extends SelectableScreen {

    public ConfigScreen(@NotNull Text title, @Nullable Screen parent) {
        super(title, parent);
    }

    @Override
    protected void initElement(ElementProvider provider) {
        provider.addToggleList((x, y, width, height) -> new ToggleWithElementButton(x, y, width, height, PostShaders.isEnableVhs(), Text.of("启用VHS"), Text.of("启用Video Home System Shader")))
                .add((x, y, width, height) -> {
                    SliderButton vhsNoiseIntensitySlider = new SliderButton(x, y, width, height, ClientChaosRigApiConfig.vhsNoiseIntensity, Text.of("VhsNoiseIntensity"), Text.of("Test"));
                    vhsNoiseIntensitySlider.setMinValue(-90); // FIXME: 不支持更细致的小数点
                    vhsNoiseIntensitySlider.setMaxValue(90);
                    //vhsNoiseIntensitySlider.setValueChangeRun(value -> ClientChaosRigApiConfig.vhsNoiseIntensity = value.floatValue());
                    return vhsNoiseIntensitySlider;
                }).processInstance(vhsToggle -> {
                    vhsToggle.setWaringMessage(Text.of("如果装载了iris等接管渲染的MOD, 该项目将不起作用"));
                    vhsToggle.setToggleRunning(PostShaders::setVhs);
                }).end();
    }
}
