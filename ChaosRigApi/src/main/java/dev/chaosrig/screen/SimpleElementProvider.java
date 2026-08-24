package dev.chaosrig.screen;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SimpleElementProvider implements ElementProvider {
    @NotNull
    protected final List<BaseButton> elements = new ArrayList<>();
    @NotNull
    protected final SelectableScreen instance;
    protected final int elementX;
    protected final int elementY;
    protected final int elementWidth;
    protected final int elementHeight;
    public final Runnable elementIndexCallback;
    public final Supplier<Integer> elementScrollOffsetCallback;

    public SimpleElementProvider(@NotNull SelectableScreen instance, int elementX, int elementY, int elementWidth, int elementHeight) {
        this.instance = instance;
        this.elementX = elementX;
        this.elementY = elementY;
        this.elementWidth = elementWidth;
        this.elementHeight = elementHeight;
        this.elementIndexCallback = instance::resetElementIndex;
        this.elementScrollOffsetCallback = instance::getScrollOffset;
    }

    @Override
    public BaseButton addBaseButton(BaseButton button) {
        this.process(button);
        return button;
    }

    @Override
    public ToggleButton addToggle(@NotNull Creator<ToggleButton> creator) {
        ToggleButton button = creator.create(this.elementX, this.elementY, this.elementWidth, this.elementHeight);
        this.process(button);
        return button;
    }

    @Override
    public ElementListBuilder<ToggleWithElementButton> addToggleList(@NotNull Creator<ToggleWithElementButton> creator) {
        return new SimpleToggleElementsListBuilder<>(this, creator);
    }

    @Override
    public SliderButton addSlider(@NotNull Creator<SliderButton> creator) {
        SliderButton button = creator.create(this.elementX, this.elementY, this.elementWidth, this.elementHeight);
        this.process(button);
        return button;
    }

    @Override
    public void clear() {
        this.elements.clear();
    }

    @Override
    public List<BaseButton> result() {
        return this.elements;
    }

    @Override
    public void process(BaseButton button) {
        button.setElementIndexCallback(this.elementIndexCallback);
        button.setScrollOffsetCallback(this.elementScrollOffsetCallback);
        this.elements.add(button);
    }

    public static class SimpleToggleElementsListBuilder<T extends ToggleWithElementButton> implements ElementListBuilder<T> {
        protected final SimpleElementProvider provider;
        protected final T instance;
        protected final List<BaseButton> elements = new ArrayList<>();

        public SimpleToggleElementsListBuilder(@NotNull SimpleElementProvider provider, @NotNull Creator<T> creator) {
            this.provider = provider;
            this.instance = creator.create(provider.elementX, provider.elementY, provider.elementWidth, provider.elementHeight);
        }

        @Override
        public T getInstance() {
            return this.instance;
        }

        @Override
        public ElementListBuilder<T> processInstance(Consumer<T> process) {
            process.accept(this.instance);
            return this;
        }

        @Override
        public ElementListBuilder<T> add(Creator<BaseButton> creator) {
            this.elements.add(creator.create(provider.elementX, provider.elementY, provider.elementWidth, provider.elementHeight));
            return this;
        }

        @Override
        public void end() {
            this.provider.addBaseButton(instance);
            this.elements.forEach(e -> {
                this.instance.addElements(e);
                this.provider.elements.add(e);
            });
            this.instance.process(false);
        }
    }
}
