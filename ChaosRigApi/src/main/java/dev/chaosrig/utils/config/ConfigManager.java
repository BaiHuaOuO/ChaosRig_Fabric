package dev.chaosrig.utils.config;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.chaosrig.ChaosRigApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.util.crash.CrashReport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfigManager {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    @NotNull
    protected final File configFile;
    @NotNull
    public final ConfigReader reader;
    @NotNull
    public final ConfigWriter writer;
    @NotNull
    protected JsonObject data = new JsonObject();

    public ConfigManager(@NotNull File file) {
        this.configFile = file;
        boolean needInit = !this.configFile.exists();
        if (needInit) {
            try {
                ChaosRigApi.LOGGER.info("创建文件({})中...", file.getName());
                this.configFile.createNewFile();
            } catch(IOException e) {
                MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create(e, "无法创建目标文件"));
            }
        }
        if (file.exists()) {
            try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                JsonElement element = GSON.fromJson(reader, JsonElement.class);
                if (element != null && element.isJsonObject()) {
                    this.data = element.getAsJsonObject();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.reader = new ConfigReader(configFile, data);
        this.writer = new ConfigWriter(configFile, data);
    }

    public void close() {
        this.reader.close();
        this.writer.close();
    }

    public void save(@NotNull Consumer<Processor> processor) {
        processor.accept(this.writer);
        this.writer.save();
    }

    public void load(@NotNull Consumer<Processor> processor) {
        processor.accept(this.reader);
    }

    /**
     * <p>配置文件读取器</p>
     * <p>
     *     键路径读取示例: 读取"value1.value2.value3", 作为{@link Boolean}, 不存在时默认值为<code>true</code> <br>
     *     文件示例:
     *     <pre>
     *         {@code
     *         {
     *             "value1": {
     *                 "value2": {
     *                     "value3": false
     *                 }
     *             }
     *         }
     *         }
     *     </pre>
     *     调用:
     *     <pre>
     *         {@code
     *         boolean valueA = READER.onBoolean("value1.value2.value3", true); // valueA = false
     *         boolean valueB = READER.onBoolean("value1.value2.abc", true); // valueB = true, 键值abc不存在
     *         }
     *     </pre>
     * </p>
     */
    public static class ConfigReader implements Processor {
        @NotNull
        protected final File file;
        protected final JsonObject data;

        protected ConfigReader(@NotNull File file) {
            this(file, null);
        }

        protected ConfigReader(@NotNull File file, @Nullable JsonObject json) {
            if (json == null) {
                json = new JsonObject();
            }
            this.data = json;
            this.file = file;
        }

        protected <T> T read(@NotNull String key, BiFunction<JsonObject, String, T> invoker) {
            String[] paths = key.split("\\.");
            int maxLengthAlsoLastIndex = paths.length - 1;
            JsonObject tmpData = this.data.getAsJsonObject();
            for (int i = 0; i < maxLengthAlsoLastIndex; i++) {
                String name = paths[i];
                JsonElement element = tmpData.get(name);
                if (element == null) {
                    JsonObject newObject = new JsonObject();
                    tmpData.add(name, newObject);
                    tmpData = newObject;
                    continue;
                }
                if (element.isJsonArray() || element.isJsonNull()) {
                    tmpData.remove(name);
                    JsonObject newObject = new JsonObject();
                    tmpData.add(name, newObject);
                    tmpData = newObject;
                    continue;
                }
                if (element.isJsonObject()) {
                    tmpData = element.getAsJsonObject();
                }
            }
            return invoker.apply(tmpData, paths[maxLengthAlsoLastIndex]);
        }

        @Override
        public @NotNull <T> SimpleOption<T> onOption(@NotNull String key, @NotNull SimpleOption<T> targetValue) {
            DataResult<T> data = targetValue.getCodec().parse(JsonOps.INSTANCE, this.data);
            data.error().ifPresent(result -> ChaosRigApi.LOGGER.error("无法读取配置项{}的值, 原因: {}", targetValue, result));
            data.result().ifPresent(targetValue::setValue);
            return targetValue;
        }

        @Override
        public @NotNull <T> T onObject(@NotNull String key, @NotNull T targetObject, @Nullable Function<JsonObject, T> decoder, @Nullable Function<T, JsonObject> encoder) {
            if (decoder == null) {
                String message = "读取对象时, 要求解码器不能为空";
                this.close();
                MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create(new RuntimeException(message), message));
                return targetObject;
            }
            JsonElement json = this.onJson(key, this.data);
            if (json.equals(this.data)) {
                return targetObject;
            }
            if (!json.isJsonObject()) {
                ChaosRigApi.LOGGER.error("目标路径中存储的Json值并非JsonObject");
                return targetObject;
            }
            T newObject = decoder.apply(this.data);
            if (newObject == null) {
                ChaosRigApi.LOGGER.error("无法从Json中还原目标对象");
                return targetObject;
            }
            return newObject;
        }

        @Override
        public int onInteger(@NotNull String key, int value) {
            return this.read(key, (json, name) -> (json.has(name) && json.get(name).isJsonPrimitive()) ? json.get(name).getAsInt() : value);
        }

        @Override
        public int[] onInteger(@NotNull String key, int[] values) {
            return this.read(key, (json, name) -> {
                if (!json.has(name) || !json.get(name).isJsonArray()) {
                    return values;
                }
                JsonArray array = json.getAsJsonArray(name);
                int[] resultArray = new int[array.size()];
                for (int i = 0; i < resultArray.length; i++) {
                    resultArray[i] = array.get(i).getAsInt();
                }
                return resultArray;
            });
        }

        @Override
        public long onLong(@NotNull String key, long value) {
            return this.read(key, (json, name) -> (json.has(name) && json.get(name).isJsonPrimitive()) ? json.get(name).getAsLong() : value);
        }

        @Override
        public long[] onLong(@NotNull String key, long[] values) {
            return this.read(key, (json, name) -> {
                if (!json.has(name) || !json.get(name).isJsonArray()) {
                    return values;
                }
                JsonArray array = json.getAsJsonArray(name);
                long[] resultArray = new long[array.size()];
                for (int i = 0; i < resultArray.length; i++) {
                    resultArray[i] = array.get(i).getAsLong();
                }
                return resultArray;
            });
        }

        @Override
        public boolean onBoolean(@NotNull String key, boolean value) {
            return this.read(key, (json, name) -> (json.has(name) && json.get(name).isJsonPrimitive()) ? json.get(name).getAsBoolean() : value);
        }

        @Override
        public boolean[] onBoolean(@NotNull String key, boolean[] values) {
            return this.read(key, (json, name) -> {
                if (!json.has(name) || !json.get(name).isJsonArray()) {
                    return values;
                }
                JsonArray array = json.getAsJsonArray(name);
                boolean[] resultArray = new boolean[array.size()];
                for (int i = 0; i < resultArray.length; i++) {
                    resultArray[i] = array.get(i).getAsBoolean();
                }
                return resultArray;
            });
        }

        @Override
        public double onDouble(@NotNull String key, double value) {
            return this.read(key, (json, name) -> (json.has(name) && json.get(name).isJsonPrimitive()) ? json.get(name).getAsDouble() : value);
        }

        @Override
        public double[] onDouble(@NotNull String key, double[] values) {
            return this.read(key, (json, name) -> {
                if (!json.has(name) || !json.get(name).isJsonArray()) {
                    return values;
                }
                JsonArray array = json.getAsJsonArray(name);
                double[] resultArray = new double[array.size()];
                for (int i = 0; i < resultArray.length; i++) {
                    resultArray[i] = array.get(i).getAsDouble();
                }
                return resultArray;
            });
        }

        @Override
        public @NotNull JsonElement onJson(@NotNull String key, @NotNull JsonElement value) {
            return this.read(key, (json, name) -> (json.has(name) && json.get(name).isJsonObject()) ? json.get(name).getAsJsonObject() : value);
        }

        @Override
        public @NotNull String onString(@NotNull String key, @NotNull String value) {
            return this.read(key, (json, name) -> (json.has(name) && json.get(name).isJsonPrimitive()) ? json.get(name).getAsString() : value);
        }

        @Override
        public @NotNull String[] onString(@NotNull String key, @NotNull String[] values) {
            return this.read(key, (json, name) -> {
                if (!json.has(name) || !json.get(name).isJsonArray()) {
                    return values;
                }
                JsonArray array = json.getAsJsonArray(name);
                String[] resultArray = new String[array.size()];
                for (int i = 0; i < resultArray.length; i++) {
                    resultArray[i] = array.get(i).getAsString();
                }
                return resultArray;
            });
        }

        @Override
        public @NotNull File getFile() {
            return this.file;
        }

        @Override
        public void close() {}
    }

    /**
     * <p>配置文件写入器</p>
     * <p>
     *     键路径写入示例: <code>value1.value2.value3</code>, 作为{@link Boolean}, 存储为<code>true</code> <br>
     *     调用:
     *     <pre>
     *         {@code
     *         WRITER.onBoolean("value1.value2.value3", true);
     *         }
     *     </pre>
     *     写入:
     *     <pre>
     *         {@code
     *         {
     *             "value1": {
     *                 "value2": {
     *                     "value3": true
     *                 }
     *             }
     *         }
     *         }
     *     </pre>
     *     若键路径存在保存值, 将会进行覆盖
     * </p>
     */
    public static class ConfigWriter implements Processor {
        @NotNull
        protected final File file;
        protected final JsonObject data;

        public ConfigWriter(@NotNull File file) {
            this(file, null);
        }

        public ConfigWriter(@NotNull File file, @Nullable JsonObject json) {
            if (json == null) {
                json = new JsonObject();
            }
            this.data = json;
            this.file = file;
        }

        protected void processJson(@NotNull String key, @NotNull BiConsumer<JsonObject, String> invoker) {
            String[] paths = key.split("\\.");
            int maxLengthAlsoLastIndex = paths.length - 1;
            JsonObject tmpData = this.data.getAsJsonObject();
            for (int i = 0; i < maxLengthAlsoLastIndex; i++) {
                String name = paths[i];
                JsonElement element = tmpData.get(name);
                if (element == null) {
                    JsonObject newObject = new JsonObject();
                    tmpData.add(name, newObject);
                    tmpData = newObject;
                    continue;
                }
                if (element.isJsonArray() || element.isJsonNull()) {
                    tmpData.remove(name);
                    JsonObject newObject = new JsonObject();
                    tmpData.add(name, newObject);
                    tmpData = newObject;
                    continue;
                }
                if (element.isJsonObject()) {
                    tmpData = element.getAsJsonObject();
                }
            }
            invoker.accept(tmpData, paths[maxLengthAlsoLastIndex]);
        }

        /**
         * <p>进行写入, 保存至本地文件中</p>
         */
        public void save() {
            try {
                JsonWriter jsonWriter = GSON.newJsonWriter(new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8));
                GSON.toJson(this.data, jsonWriter);
                jsonWriter.flush();
            } catch(IOException e) {
                MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create(e, "初始化写入器发生错误"));
            }
        }

        /**
         * <p>处理一个{@link SimpleOption}对象, 将{@link SimpleOption}序列化为<code>json</code></p>
         * @param key 键路径
         * @param option 写入对象
         * @return 写入值
         * @param <T> {@link SimpleOption}传参泛型任意对象
         */
        @Override
        public @NotNull <T> SimpleOption<T> onOption(@NotNull String key, @NotNull SimpleOption<T> option) {
            DataResult<JsonElement> data = option.getCodec().encodeStart(JsonOps.INSTANCE, option.getValue());
            data.error().ifPresent(result -> ChaosRigApi.LOGGER.error("无法将配置项{}的值写入, 原因: {}", option, result));
            data.result().ifPresent(json -> this.onJson(key, json));
            return option;
        }

        /**
         * <p>写入一个对象, 通过<code>encoder</code>手动记录<code>json</code></p>
         * @param key 键路径
         * @param writeValue 写入对象
         * @param decoder [不必要] 解码器, 用于还原对象
         * @param encoder 编码器, 用于将对象序列化
         * @return 写入值
         * @param <T> 任意对象
         */
        @Override
        public @NotNull <T> T onObject(@NotNull String key, @NotNull T writeValue, @Nullable Function<JsonObject, T> decoder, @Nullable Function<T, JsonObject> encoder) {
            if (encoder == null) {
                String message = "写入对象时, 要求编码器不能为空";
                this.close();
                MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create(new RuntimeException(message), message));
                return writeValue;
            }
            JsonObject json = encoder.apply(writeValue);
            if (json == null) {
                ChaosRigApi.LOGGER.error("无法通过传参对象写入相应Json");
                return writeValue;
            }
            this.onJson(key, json);
            return writeValue;
        }

        /**
         * <p>写入一个{@link Integer}类型</p>
         * @param key 键路径
         * @param writeValue 写入值
         * @return 写入值
         */
        @Override
        public int onInteger(@NotNull String key, int writeValue) {
            this.processJson(key, (json, name) -> json.addProperty(name, writeValue));
            return writeValue;
        }

        /**
         * <p>写入一组{@link Integer}并作为数组存储</p>
         * @param key 键路径
         * @param writeValues 写入值
         * @return 写入值
         */
        @Override
        public int[] onInteger(@NotNull String key, int[] writeValues) {
            this.processJson(key, (json, name) -> {
                JsonArray array = new JsonArray();
                for (int writeValue : writeValues) {
                    array.add(writeValue);
                }
                json.add(name, array);
            });
            return writeValues;
        }

        /**
         * <p>写入一个{@link Long}类型</p>
         * @param key 键路径
         * @param writeValue 写入值
         * @return 写入值
         */
        @Override
        public long onLong(@NotNull String key, long writeValue) {
            this.processJson(key, (json, name) -> json.addProperty(name, writeValue));
            return writeValue;
        }

        /**
         * <p>写入一组{@link Long}并作为数组存储</p>
         * @param key 键路径
         * @param writeValues 写入值
         * @return 写入值
         */
        @Override
        public long[] onLong(@NotNull String key, long[] writeValues) {
            this.processJson(key, (json, name) -> {
                JsonArray array = new JsonArray();
                for (long writeValue : writeValues) {
                    array.add(writeValue);
                }
                json.add(name, array);
            });
            return writeValues;
        }

        /**
         * <p>写入一个{@link Boolean}类型</p>
         * @param key 键路径
         * @param writeValue 写入值
         * @return 写入值
         */
        @Override
        public boolean onBoolean(@NotNull String key, boolean writeValue) {
            this.processJson(key, (json, name) -> json.addProperty(name, writeValue));
            return writeValue;
        }

        /**
         * <p>写入一组{@link Boolean}并作为数组存储</p>
         * @param key 键路径
         * @param writeValues 写入值
         * @return 写入值
         */
        @Override
        public boolean[] onBoolean(@NotNull String key, boolean[] writeValues) {
            this.processJson(key, (json, name) -> {
                JsonArray array = new JsonArray();
                for (boolean writeValue : writeValues) {
                    array.add(writeValue);
                }
                json.add(name, array);
            });
            return writeValues;
        }

        /**
         * <p>写入一个{@link Double}类型</p>
         * @param key 键路径
         * @param writeValue 写入值
         * @return 写入值
         */
        @Override
        public double onDouble(@NotNull String key, double writeValue) {
            this.processJson(key, (json, name) -> json.addProperty(name, writeValue));
            return writeValue;
        }

        /**
         * <p>写入一组{@link Double}并作为数组存储</p>
         * @param key 键路径
         * @param writeValues 写入值
         * @return 写入值
         */
        @Override
        public double[] onDouble(@NotNull String key, double[] writeValues) {
            this.processJson(key, (json, name) -> {
                JsonArray array = new JsonArray();
                for (double writeValue : writeValues) {
                    array.add(writeValue);
                }
                json.add(name, array);
            });
            return writeValues;
        }

        /**
         * <p>在目标键路径后添加<code>json</code>内容</p>
         * @param key 键路径
         * @param writeValue 写入值
         * @return 写入值
         */
        @Override
        public @NotNull JsonElement onJson(@NotNull String key, @NotNull JsonElement writeValue) {
            this.processJson(key, (json, name) -> json.add(name, writeValue));
            return writeValue;
        }

        /**
         * <p>写入一个{@link String}类型</p>
         * @param key 键路径
         * @param writeValue 写入值
         * @return 写入值
         */
        @Override
        public @NotNull String onString(@NotNull String key, @NotNull String writeValue) {
            this.processJson(key, (json, name) -> json.addProperty(name, writeValue));
            return writeValue;
        }

        /**
         * <p>写入一组{@link String}并作为数组存储</p>
         * @param key 键路径
         * @param writeValues 写入值
         * @return 写入值
         */
        @Override
        public @NotNull String[] onString(@NotNull String key, @NotNull String[] writeValues) {
            this.processJson(key, (json, name) -> {
                JsonArray array = new JsonArray();
                for (String writeValue : writeValues) {
                    array.add(writeValue);
                }
                json.add(name, array);
            });
            return writeValues;
        }

        @Override
        public @NotNull File getFile() {
            return this.file;
        }

        @Override
        public void close() {
            this.save();
        }
    }

    /**
     * <p>一个基础处理对象的格式接口</p>
     */
    public interface Processor {

        @NotNull
        <T> SimpleOption<T> onOption(@NotNull String key, @NotNull SimpleOption<T> option);

        @NotNull
        <T> T onObject(@NotNull String key, @NotNull T object, @Nullable Function<JsonObject, T> decoder, @Nullable Function<T, JsonObject> encoder);

        int onInteger(@NotNull String key, int value);

        int[] onInteger(@NotNull String key, int[] values);

        long onLong(@NotNull String key, long value);

        long[] onLong(@NotNull String key, long[] values);

        boolean onBoolean(@NotNull String key, boolean value);

        boolean[] onBoolean(@NotNull String key, boolean[] values);

        double onDouble(@NotNull String key, double value);

        double[] onDouble(@NotNull String key, double[] values);

        @NotNull
        JsonElement onJson(@NotNull String key, @NotNull JsonElement value);

        @NotNull
        String onString(@NotNull String key, @NotNull String value);

        @NotNull
        String[] onString(@NotNull String key, @NotNull String[] values);

        /**
         * <p>获取目标文件对象</p>
         * @return 目标文件
         */
        @NotNull
        File getFile();

        /**
         * <p>当{@link ConfigManager}执行{@link ConfigManager#close()}时, 执行器关闭时需要做什么</p>
         */
        void close();
    }
}
