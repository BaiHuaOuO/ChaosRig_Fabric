package dev.chaosrig.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chaosrig.ChaosRigApi;
import dev.chaosrig.ChaosRigClient;
import dev.chaosrig.utils.renderer.InformationScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class TooltipReader {
    public static final String FILE_NAME = "tooltips";
    public static final Identifier FILE_PATH = Objects.requireNonNull(Identifier.of(ChaosRigApi.MAIN_MOD_ID, FILE_NAME + ".json"));
    public static final Text UNDEFINE_TEXT = Text.of("undefine");
    @Nullable
    public static JsonArray root = null;

    public static void register() {
        if (ChaosRigClient.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        ResourceHelper.CLIENT_RESOURCE_RELOAD_EVENT.register(TooltipReader::init);
    }

    @NotNull
    public static Text[] read(@NotNull Item item) {
        JsonObject object = getJson(item);
        Text info = readWithInfo(object);
        Text[] details = readWithDetails(object);
        Text[] newText = new Text[details.length + 1];
        newText[0] = info;
        System.arraycopy(details, 0, newText, 1, details.length);
        return newText;
    }

    public static Text[] read(@NotNull Block block) {
        return read(block.asItem());
    }

    @NotNull
    public static Text readWithInfo(@NotNull Item item) {
        return readWithInfo(getJson(item));
    }

    public static Text readWithInfo(@NotNull Block block) {
        return readWithInfo(block.asItem());
    }

    @NotNull
    protected static Text readWithInfo(@Nullable JsonObject json) {
        return json == null
                ? UNDEFINE_TEXT
                : json.has("info")
                    ? Text.translatable(json.get("info").getAsString())
                    : UNDEFINE_TEXT;
    }

    @NotNull
    protected static Text[] readWithDetails(@Nullable JsonObject json) {
        if (json == null) {
            return new Text[] {UNDEFINE_TEXT};
        }
        if (!json.has("details")) {
            return new Text[] {UNDEFINE_TEXT};
        }
        JsonArray array = json.getAsJsonArray("details");
        Text[] texts = new Text[array.size()];
        for (int i = 0; i < array.size(); i++) {
            texts[i] = Text.translatable(array.get(i).getAsString());
        }
        return texts;
    }

    @Nullable
    protected static JsonObject getJson(@NotNull Item item) {
        if (root == null) {
            return null;
        }
        for (int i = 0; i < root.size(); i++) {
            JsonObject object = root.get(i).getAsJsonObject();
            if (!object.has("item")) {
                continue;
            }
            if (object.get("item").getAsString().equals(Registries.ITEM.getId(item).toString())) {
                return object;
            }
        }
        return null;
    }

    protected static void init(ResourceManager manager) {
        try (BufferedReader reader = ResourceHelper.getReader(manager, FILE_PATH).orElse(null)) {
            if (reader == null) {
                return;
            }
            root = JsonParser.parseReader(reader).getAsJsonArray();
        } catch(IOException e) {
            ChaosRigApi.LOGGER.warn("无法加载BufferedReader: ", e);
        }
    }
}
