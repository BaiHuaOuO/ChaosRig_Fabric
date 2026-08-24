package dev.chaosrig.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chaosrig.ChaosRigApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;

@Environment(EnvType.CLIENT)
public class TooltipReader {
    public static final String FILE_NAME = "tooltips";
    public static final Identifier FILE_PATH = Identifier.of(ChaosRigApi.MAIN_MOD_ID, FILE_NAME + ".json");
    public static final Text UNDEFINE_TEXT = Text.of("undefine");

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
        BufferedReader reader = ResourceHelper.getReader(FILE_PATH).orElse(null);
        if (reader == null) {
            return null;
        }
        JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            if (!object.has("item")) {
                continue;
            }
            if (object.get("item").getAsString().equals(Registries.ITEM.getId(item).toString())) {
                return object;
            }
        }
        return null;
    }
}
