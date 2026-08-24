package dev.chaosrig.utils.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>注解一个字段变量, 当有任意一名玩家加入服务器时, 服务器发送{@link dev.chaosrig.utils.PacketList#SERVER_SEND_SYNC_VALUE}数据包进行同步数据</p>
 * <p>
 *     此注解需要搭配注解{@link SyncFromServer}进行使用, 示例: <br>
 *     <code>ExampleA.java</code>
 *     <pre>
 *         {@code
 *         @SyncToClient(type = SyncType.Integer)
 *         public static int maxValue = 50; // 服务端发送数据
 *
 *         public static void register() {
 *             ProcessSyncAnnotation.addProvider(ExampleA.class); // 确保注解字段所在的类被注册进{@link ProcessSyncAnnotation}
 *         }
 *         }
 *     </pre>
 *     <code>ExampleB.java</code>
 *     <pre>
 *         {@code
 *         @SyncFromServer(type = SyncType.Integer)
 *         public static int maxValue = 0; // 客户端默认数值, 同时确保字段名称与服务端名称一致
 *
 *         public static void register() {
 *             ReceiveSyncAnnotation.addProvider(ExampleB.class); // 确保注解字段所在的类被注册进{@link ReceiveSyncAnnotation}
 *         }
 *         }
 *     </pre>
 *     当任意玩家加入游戏时, 服务端会进行<code>maxValue</code>(服务端与客户端字段名称必须一致, 否则不会被查找到)的同步
 * </p>
 * <p>若服务端不存在<code>ChaosRigApi</code>时, 该注解不会生效, 需要自行初始化赋值</p>
 * @see ProcessSyncAnnotation
 * @see ReceiveSyncAnnotation
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SyncToClient {

    /**
     * 同步数据类型
     */
    SyncType type();
}
