package dev.chaosrig.screen;

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ToggleWithElementButton extends ToggleButton {
    protected final List<BaseButton> elements = new ArrayList<>();

    public ToggleWithElementButton(int x, int y, int width, int height, boolean defaultToggle, @NotNull Text message, @Nullable Text description) {
        super(x, y, width, height, defaultToggle, message, description);
    }

    @Override
    public void toggle() {
        super.toggle();
        if (!this.lock) {
            this.process(true);
        }
    }

    public void process(boolean call) {
        this.elements.forEach(e -> e.setNarratable(this.toggle));
        if (call && this.elementIndexCallback != null) {
            this.elementIndexCallback.run();
        }
    }

    public void addElements(@NotNull BaseButton baseButton) {
        this.elements.add(baseButton);
    }

    public List<BaseButton> getElements() {
        return this.elements;
    }
}
