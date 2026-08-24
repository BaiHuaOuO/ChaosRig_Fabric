package dev.chaosrig.utils.config;

import dev.chaosrig.ChaosRigApi;
import dev.chaosrig.utils.PacketList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Queue;

/**
 * <p>进行处理目标类的{@link SyncToClient}注解</p>
 */
public class ProcessSyncAnnotation {
    protected static final Queue<Class<?>> providers = new LinkedList<>();
    protected static final ArrayList<Record> records = new ArrayList<>();

    public static void register() {
        if (ChaosRigApi.isInit()) throw new RuntimeException("不允许重复注册");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> init());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(sender));
    }

    /**
     * 初始化/重新获取同步数据
     */
    public static void init() {
        records.clear();
        for (Class<?> c : providers) {
            for (Field field : c.getFields()) {
                if (!field.isAnnotationPresent(SyncToClient.class)) continue;
                SyncToClient syncAnnotation = field.getAnnotation(SyncToClient.class);
                try {
                    field.setAccessible(true);
                    records.add(new Record(field.getName(), syncAnnotation.type(), field.get(c)));
                } catch(IllegalAccessException e) {
                    ChaosRigApi.LOGGER.warn("无法获得目标字段内的值: {}({})", field, c);
                }
            }
        }
    }

    /**
     * 添加目标类
     * @param target 监听目标
     */
    public static void addProvider(@NotNull Class<?> target) {
        providers.add(target);
    }

    /**
     * 添加目标类
     * @param target 监听目标
     */
    public static void addProvider(@NotNull Object target) {
        addProvider(target.getClass());
    }

    protected static void process(PacketByteBuf buf) {
        buf.writeInt(records.size());
        for (Record record : records) {
            buf.writeString(record.name);
            buf.writeEnumSet(EnumSet.of(record.type), SyncType.class);
            Object value = record.value;
            switch (record.type) {
                case Integer -> buf.writeInt((int) value);
                case Long -> buf.writeLong((long) value);
                case Double -> buf.writeDouble((double) value);
                case String -> buf.writeString((String) value);
                case Boolean -> buf.writeBoolean((boolean) value);
                case Json -> buf.writeString(value.toString());
            }
        }
    }

    /**
     * 向玩家发送同步数据
     * @param player 目标玩家
     */
    public static void send(@NotNull ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        process(buf);
        ServerPlayNetworking.send(player, PacketList.SERVER_SEND_SYNC_VALUE, buf);
    }

    /**
     * 向玩家发送同步数据
     * @param sender 目标玩家客户端
     */
    public static void send(@NotNull PacketSender sender) {
        PacketByteBuf buf = PacketByteBufs.create();
        process(buf);
        sender.sendPacket(PacketList.SERVER_SEND_SYNC_VALUE, buf);
    }

    public record Record(@NotNull String name, @NotNull SyncType type, @NotNull Object value) {}
}
