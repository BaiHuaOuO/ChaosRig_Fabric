package dev.chaosrig.screen;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public interface ElementProvider {

    BaseButton addBaseButton(BaseButton button);

    ToggleButton addToggle(@NotNull Creator<ToggleButton> creator);

    ElementListBuilder<ToggleWithElementButton> addToggleList(@NotNull Creator<ToggleWithElementButton> creator);

    SliderButton addSlider(@NotNull Creator<SliderButton> creator);

    void clear();

    List<BaseButton> result();

    void process(BaseButton button);

    @FunctionalInterface
    interface Creator<T extends BaseButton> {
        T create(int x, int y, int width, int height);
    }

    interface ElementListBuilder<T extends BaseButton> {

        T getInstance();

        ElementListBuilder<T> processInstance(Consumer<T> process);

        ElementListBuilder<T> add(Creator<BaseButton> creator);

        void end();
    }
}
