package dev.chaosrig.utils;

import dev.chaosrig.ChaosRigApi;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class PacketList {
    /**
     * 客户端Api模组尝试确认服务端装载了Api模组
     */
    public static Identifier CLIENT_CHECKING_SERVER_EXIST = build(From.API_MOD, 0);
    /**
     * 服务端返回已装载Api模组的信号
     */
    public static Identifier CLIENT_CHECKED_SERVER_IS_EXIST = build(From.API_MOD, 1);
    /**
     * 客户端同步RigInventory选择槽
     */
    public static Identifier CLIENT_SYNC_RIG_INVENTORY_SELECT_SLOT = build(From.MAIN_MOD, 2);
    /**
     * 服务端同步RigInventory
     */
    public static Identifier SERVER_SYNC_RIG_INVENTORY = build(From.MAIN_MOD, 3);
    /**
     * 服务端同步Ping数据
     */
    public static Identifier SERVER_SYNC_PING_DATA = build(From.API_MOD, 4);
    /**
     * 客户端发送ping操作
     */
    public static Identifier CLIENT_PING = build(From.API_MOD, 5);
    /**
     * 客户端发送取消ping信息操作
     */
    public static Identifier CLIENT_CANCEL_PING = build(From.API_MOD, 6);
    /**
     * 服务器对客户端进行同步配置文件内容
     */
    public static Identifier SERVER_SEND_SYNC_VALUE = build(From.API_MOD, 10);

    protected enum From {
        MAIN_MOD,
        API_MOD;
    }

    @NotNull
    protected static Identifier build(@NotNull From from, int number) {
        Identifier id = Identifier.of(from == From.MAIN_MOD ? ChaosRigApi.MAIN_MOD_ID : ChaosRigApi.API_MOD_ID, "packet_" + number);
        if (id == null) throw new NullPointerException("Identifier Packet was created but null");
        return id;
    }
}
