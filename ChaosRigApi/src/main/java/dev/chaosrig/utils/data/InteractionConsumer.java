package dev.chaosrig.utils.data;

import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface InteractionConsumer {

    /**
     * <p>是否属于客户端</p>
     * @return 是与否
     */
    boolean isClient();

    /**
     * <p>是否应该进行同步</p>
     *
     * @return 是与否
     */
    boolean shouldSync();

    /**
     * <p>标记应该同步</p>
     */
    void markShouldSync();

    /**
     * <p>进行同步数据</p>
     * @param world 处理者传参世界类型({@link net.minecraft.client.world.ClientWorld}), 服务端默认传参为<code>null</code>
     */
    void syncData(@Nullable World world);

    /**
     * <p>一个<code>tick</code>段进行一次更新</p>
     * <p>
     *     {@link InteractionConsumer#shouldTickUpdate()}为<code>false</code>时, 该方法不会被执行
     * </p>
     * @param world 处理者传参世界类型({@link net.minecraft.client.world.ClientWorld}), 服务端默认传参为<code>null</code>
     */
    void tickUpdate(@Nullable World world);

    /**
     * <p>应该每个<code>tick</code>段执行一次{@link InteractionConsumer#tickUpdate(World)}</p>
     * @return 是与否
     */
    default boolean shouldTickUpdate() {
        return true;
    }

    /**
     * <p>一<code>秒</code>时段进行一次更新</p>
     * <p>
     *     {@link InteractionConsumer#shouldSecondUpdate()}为<code>false</code>时, 该方法不会被执行
     * </p>
     */
    default void secondUpdate() {}

    /**
     * <p>应该每<code>秒</code>段执行一次{@link InteractionConsumer#secondUpdate()} ()}</p>
     * @return 是与否
     */
    default boolean shouldSecondUpdate() {
        return false;
    }
}
