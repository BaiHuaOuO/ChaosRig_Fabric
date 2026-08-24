package dev.chaosrig.utils.data;

import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

/**
 * <p>一个用于对{@link InteractionConsumer}集合更新和同步网络数据</p>
 */
public class InteractionManager {
    private short tick = 0;
    protected final boolean isClient;
    protected Queue<InteractionConsumer> consumers = new ConcurrentLinkedQueue<>();

    public InteractionManager(boolean isClient) {
        this.isClient = isClient;
    }

    /**
     * 是否为客户端方专属
     * @return 是与否
     */
    public boolean isClient() {
        return isClient;
    }

    /**
     * <p>尝试在当前集合内查找到目标对象</p>
     * <p>示例用法:<pre>
     *     {@code
     *     A a1 = new A();
     *     interactionManager.addConsumer(a1);
     *     A a2 = interactionManager.find(A.class).findFirst().orElseThrow(); // a1 == a2
     *     }
     * </pre></p>
     * @param target 目标对象
     * @return 对象集合, 可能为空集
     * @param <T> 对象集合类型
     */
    @NotNull
    public <T extends InteractionConsumer> Stream<T> find(Class<T> target) {
        return consumers.stream()
                .filter(target::isInstance)
                .map(target::cast)
                .toList()
                .stream();
    }

    /**
     * 清除集合内容
     */
    public void clear() {
        this.consumers.clear();
    }

    /**
     * 移除一个对象
     * @param consumer 目标
     */
    public void removeConsumer(@NotNull InteractionConsumer consumer) {
        this.consumers.remove(consumer);
    }

    /**
     * 移除集合内所有关于该对象的元素
     * @param target 目标Class类
     * @param <T> 对象类型
     */
    public <T extends InteractionConsumer> void remove(Class<T> target) {
        this.find(target).forEach(this::removeConsumer);
    }

    /**
     * 添加一个对象
     * @param consumer 对象
     * @throws IllegalArgumentException 添加与环境不相配的数据处理器
     */
    public void addConsumer(@NotNull InteractionConsumer consumer) {
        if (consumer.isClient() != this.isClient) {
            throw new IllegalArgumentException("添加与环境不相配的数据处理器");
        }
        this.consumers.add(consumer);
    }

    public void tick(@Nullable World world) {
        if (consumers.isEmpty()) return;
        for (InteractionConsumer consumer : consumers) {
            if (consumer.shouldTickUpdate()) consumer.tickUpdate(world);
            if (consumer.shouldSync()) consumer.syncData(world);
            if (tick >= 19) {
                if (consumer.shouldSecondUpdate()) consumer.secondUpdate();
            }
        }
        tick++;
        if (tick >= 20) {
            tick = 0;
        }
    }
}