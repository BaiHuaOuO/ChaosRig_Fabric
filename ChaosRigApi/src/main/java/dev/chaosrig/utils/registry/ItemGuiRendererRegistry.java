package dev.chaosrig.utils.registry;

import dev.chaosrig.item.ItemGuiRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class ItemGuiRendererRegistry implements RegistryMap<ItemGuiRenderer<?>, Item> {
    protected final List<ItemGuiRenderer<?>> renderers = Collections.synchronizedList(new ArrayList<>());

    public void add(ItemGuiRenderer<?> object) {
        renderers.add(object);
    }

    @Override
    public void add(@NotNull ItemGuiRenderer<?> object, @Nullable Item value) {
        Objects.requireNonNull(object);
        renderers.add(object);
    }

    @Override
    public @NotNull ItemGuiRenderer<?>[] array() {
        return this.renderers.toArray(ItemGuiRenderer[]::new);
    }

    @Override
    public @NotNull Stream<ItemGuiRenderer<?>> get(@NotNull Item value) {
        return renderers.stream().filter(i -> isEqualItem(i.getItem(), value));
    }

    public static boolean isEqualItem(Item i1, Item i2) {
        return Item.getRawId(i1) == Item.getRawId(i2);
    }
}
