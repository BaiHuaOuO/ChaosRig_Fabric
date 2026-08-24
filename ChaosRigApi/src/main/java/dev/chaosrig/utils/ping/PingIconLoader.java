package dev.chaosrig.utils.ping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chaosrig.ChaosRigApi;
import dev.chaosrig.utils.ResourceHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * @deprecated 由于IO开销过大, 需要重构
 */
@Deprecated
@Environment(EnvType.CLIENT)
public class PingIconLoader {
    public static final String PING_ICON_DIR_NAME = "ping_tag";
    public static final String ENTITY_DIR_NAME = "entity";
    protected static final List<String> modsId = new ArrayList<>();
    public static final Identifier DEFAULT_ENTITY_ICON = new Identifier(ChaosRigApi.API_MOD_ID, "textures/" + PING_ICON_DIR_NAME + "/default_entity.png");

    public static void registerMod(@NotNull String modId) {
        modsId.add(modId);
    }

    static {
        registerMod(ChaosRigApi.API_MOD_ID);
        registerMod(ChaosRigApi.MAIN_MOD_ID);
    }

    @NotNull
    public static Stream<Identifier> getIconName(@NotNull String fileName, @NotNull Identifier entityId) {
        return modsId.stream()
                .flatMap(i -> ResourceHelper.getReader(new Identifier(i, PING_ICON_DIR_NAME + '/' + fileName + ".json")).stream())
                .flatMap(r -> {
                    JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
                    JsonElement icon = json.get(entityId.toString());
                    return icon == null ? Stream.empty() : Stream.of(new Identifier(icon.getAsString()));
                });
    }

    @Nullable
    public static Identifier getIconNameRandom(@NotNull String fileName, @NotNull Identifier entityId) {
        Identifier[] icons = getIconName(fileName, entityId).toArray(Identifier[]::new);
        return icons.length == 0 ? null : icons[new Random(System.currentTimeMillis()).nextInt(icons.length)];
    }

    @NotNull
    public static Identifier getEntity(@NotNull Entity entity) {
        Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());
        Identifier icon = getIconNameRandom(ENTITY_DIR_NAME, id);
        return icon == null ? DEFAULT_ENTITY_ICON : icon;
    }

}
