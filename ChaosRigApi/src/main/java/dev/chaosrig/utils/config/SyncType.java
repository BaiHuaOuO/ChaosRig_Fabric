package dev.chaosrig.utils.config;

/**
 * <p>注解{@link SyncFromServer}和{@link SyncToClient}记录所需要的数据类型</p>
 */
public enum SyncType {
    /**
     * <code>int</code>类型
     */
    Integer,
    /**
     * <code>double</code>类型
     */
    Double,
    /**
     * <code>long</code>类型
     */
    Long,
    /**
     * <code>String</code>类型
     */
    String,
    /**
     * <code>boolean</code>类型
     */
    Boolean,
    /**
     * {@link com.google.gson.JsonObject}类型
     */
    Json
}
