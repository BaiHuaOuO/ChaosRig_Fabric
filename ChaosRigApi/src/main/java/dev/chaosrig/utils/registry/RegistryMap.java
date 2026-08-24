package dev.chaosrig.utils.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public interface RegistryMap<T, R> {

    void add(@NotNull T object, R value);

    @NotNull
    T[] array();

    @NotNull
    Stream<T> get(@NotNull R value);

    @Nullable
    default T getOnly(@NotNull R value) {
        Stream<T> objects = this.get(value);
        List<T> list = objects.limit(2).toList();
        if (list.size() > 1) {
            throw new IllegalArgumentException("不满足单一变量");
        }
        return (list.isEmpty()) ? null : list.get(0);
    }
}
