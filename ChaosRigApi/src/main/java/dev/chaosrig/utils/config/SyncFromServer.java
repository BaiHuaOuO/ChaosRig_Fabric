package dev.chaosrig.utils.config;

import dev.chaosrig.ChaosRigApiClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>注解一个字段变量, 当接收到{@link dev.chaosrig.utils.PacketList#SERVER_SEND_SYNC_VALUE}数据包时, 会尝试同步数据(如果存在)</p>
 * <p>
 *     此注解需要搭配注解{@link SyncToClient}进行使用, 示例: <br>
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
 *     当客户端加入游戏时, <code>maxValue</code>(服务端与客户端字段名称必须一致, 否则不会被查找到)将会被同步为<code>50</code>
 * </p>
 * <p>
 *     该注解在{@link ChaosRigApiClient#isServerExist()}为<code>false</code>(即服务端未装载<code>ChaosRigApi</code>MOD)时失效, 需要开发者自行处理该变量的管理
 * </p>
 * @see ProcessSyncAnnotation
 * @see ReceiveSyncAnnotation
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Environment(EnvType.CLIENT)
public @interface SyncFromServer {

    /**
     * 同步类型
     */
    SyncType type();
}
