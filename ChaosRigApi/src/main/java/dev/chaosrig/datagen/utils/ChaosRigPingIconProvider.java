package dev.chaosrig.datagen.utils;

import com.google.gson.JsonObject;
import dev.chaosrig.utils.ping.PingIconLoader;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class ChaosRigPingIconProvider implements DataProvider {
    protected final FabricDataOutput output;

    public ChaosRigPingIconProvider(@NotNull FabricDataOutput output) {
        this.output = output;
    }

    protected abstract void process(@NotNull Builder builder);

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        Builder builder = new Builder();
        this.process(builder);
        return DataProvider.writeToPath(writer, builder.resultEntity(), getEntityFilePath());
    }

    protected Path getEntityFilePath() {
        return output.getResolver(DataOutput.OutputType.RESOURCE_PACK, PingIconLoader.PING_ICON_DIR_NAME)
                .resolveJson(Identifier.of(output.getModId(), PingIconLoader.ENTITY_DIR_NAME));
    }

    @Override
    public String getName() {
        return "PingIcon";
    }

    public static class Builder {
        protected final Map<Identifier, Identifier> entities = new HashMap<>();

        public void addEntity(@NotNull Identifier entityId, @NotNull Identifier iconPath) {
            this.entities.put(entityId, iconPath);
        }

        public void addEntity(@NotNull Entity entity, @NotNull Identifier iconPath) {
            addEntity(Registries.ENTITY_TYPE.getId(entity.getType()), iconPath);
        }

        @NotNull
        protected JsonObject resultEntity() {
            JsonObject root = new JsonObject();
            entities.forEach((entity,path) -> root.addProperty(entity.toString(), path.toString()));
            return root;
        }
    }
}
