package dev.chaosrig.utils.config;

import dev.chaosrig.ChaosRigApi;
import dev.chaosrig.ChaosRigApiClient;
import dev.chaosrig.utils.PacketList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

/**
 * <p>处理注解{@link SyncFromServer}并同步数据</p>
 */
@Environment(EnvType.CLIENT)
public class ReceiveSyncAnnotation {
    protected static final Queue<Class<?>> consumers = new LinkedList<>();
    protected static final ArrayList<ProcessSyncAnnotation.Record> records = new ArrayList<>();

    public static void register() {
        if (ChaosRigApiClient.isInit()) throw new RuntimeException("不允许重复注册");
        ClientPlayNetworking.registerGlobalReceiver(PacketList.SERVER_SEND_SYNC_VALUE, ReceiveSyncAnnotation::receiveBuf);
    }

    /**
     * 添加目标类
     * @param target 监听目标
     */
    public static void addConsumer(@NotNull Class<?> target) {
        consumers.add(target);
    }

    /**
     * 添加目标类
     * @param target 监听目标
     */
    public static void addConsumer(@NotNull Object target) {
        addConsumer(target.getClass());
    }

    protected static void receiveBuf(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String name = buf.readString();
            SyncType type = buf.readEnumSet(SyncType.class).stream().findFirst().get();
            Object value = switch (type) {
                case Integer -> buf.readInt();
                case Double -> buf.readDouble();
                case Long -> buf.readLong();
                case String, Json -> buf.readString();
                case Boolean -> buf.readBoolean();
            };
            records.add(new ProcessSyncAnnotation.Record(name, type, value));
        }
        process();
    }

    /**
     * 从剩余记录数据({@link ReceiveSyncAnnotation#records})进行同步处理
     */
    public static void process() {
        if (records.isEmpty()) {
            return;
        }
        for (Class<?> c : consumers) {
            for (Field field : c.getFields()) {
                if (!field.isAnnotationPresent(SyncFromServer.class)) continue;
                String name = field.getName();
                Iterator<ProcessSyncAnnotation.Record> recordIterator = records.iterator();
                while (recordIterator.hasNext()) {
                    ProcessSyncAnnotation.Record record = recordIterator.next();
                    if (!record.name().equals(name)) continue;
                    field.setAccessible(true);
                    try {
                        field.set(c, record.value());
                        recordIterator.remove();
                    } catch (IllegalAccessException e) {
                        ChaosRigApi.LOGGER.warn("无法同步设置目标字段: {}({})", field, c);
                    }
                }
            }
        }
        if (!records.isEmpty()) {
            ChaosRigApi.LOGGER.warn("剩余字段尚未被同步设置: {}", Arrays.toString(records.toArray()));
        }
    }
}
