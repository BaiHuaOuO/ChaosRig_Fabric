package dev.chaosrig.utils;

import dev.chaosrig.ChaosRigApi;
import dev.chaosrig.ChaosRigApiClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class ResourceHelper {
    public static Event<ClientResourceReloadEvent> CLIENT_RESOURCE_RELOAD_EVENT = EventFactory.createArrayBacked(ClientResourceReloadEvent.class, clientResourceReloadEvents -> manager -> {
        for (ClientResourceReloadEvent event : clientResourceReloadEvents) {
            event.run(manager);
        }
    });
    public static Event<ServerDataReloadEvent> SERVER_DATA_RELOAD_EVENT = EventFactory.createArrayBacked(ServerDataReloadEvent.class, serverDataReloadEvents -> manager -> {
        for (ServerDataReloadEvent event : serverDataReloadEvents) {
            event.run(manager);
        }
    });

    public static void register() {
        if (ChaosRigApiClient.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        registerReload();
    }

    protected static void registerReload() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(ChaosRigApi.API_MOD_ID, "client_json_reload");
            }

            @Override
            public void reload(ResourceManager manager) {
                SERVER_DATA_RELOAD_EVENT.invoker().run(manager);
            }
        });
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(ChaosRigApi.API_MOD_ID, "client_json_reload");
            }

            @Override
            public void reload(ResourceManager manager) {
                CLIENT_RESOURCE_RELOAD_EVENT.invoker().run(manager);
            }
        });
    }

    @NotNull
    public static Optional<Resource> getResource(@NotNull ResourceManager manager, @NotNull Identifier modResource) {
        return manager.getResource(modResource);
    }

    /**
     * <p>临时获取一个文件实例</p>
     * <p>禁止频繁调用</p>
     * @param modResource 目标路径
     * @return {@link Resource}对象, 文件不存在为<code>Optional.empty()</code>
     */
    @NotNull
    public static Optional<Resource> getResource(@NotNull Identifier modResource) {
        return getResource(MinecraftClient.getInstance().getResourceManager(), modResource);
    }

    public static Optional<InputStream> getInputStream(@NotNull ResourceManager manager, @NotNull Identifier modResource) {
        return getResource(manager, modResource).flatMap(r -> {
            try {
                return Optional.of(r.getInputStream());
            } catch (IOException e) {
                ChaosRigApi.LOGGER.error("对象:\"{}\"的InputStream获取失败: {}", modResource, e);
                return Optional.empty();
            }
        });
    }

    /**
     * <p>临时获取一个文件的输入流</p>
     * <p>禁止频繁调用</p>
     * @param modResource 目标路径
     * @return {@link InputStream}对象, 文件不存在为<code>Optional.empty()</code>
     */
    @NotNull
    public static Optional<InputStream> getInputStream(@NotNull Identifier modResource) {
        return getInputStream(MinecraftClient.getInstance().getResourceManager(), modResource);
    }

    /**
     * <p>临时获取一个文件的缓存输出流</p>
     * <p>禁止频繁调用</p>
     * @param modResource 目标路径
     * @return {@link BufferedReader}对象, 文件不存在为<code>Optional.empty()</code>
     */
    @NotNull
    public static Optional<BufferedReader> getReader(@NotNull Identifier modResource) {
        return getReader(MinecraftClient.getInstance().getResourceManager(), modResource);
    }

    @NotNull
    public static Optional<BufferedReader> getReader(@NotNull ResourceManager manager, @NotNull Identifier modResource) {
        return getResource(manager, modResource).flatMap(r -> {
            try {
                return Optional.of(r.getReader());
            } catch (IOException e) {
                ChaosRigApi.LOGGER.error("对象:\"{}\"的BufferedReader获取失败: {}", modResource, e);
                return Optional.empty();
            }
        });
    }

    public interface ReloadEvent {

        void run(ResourceManager manager);

        ResourceType getReloadType();
    }

    @FunctionalInterface
    public interface ClientResourceReloadEvent extends ReloadEvent {
        @Override
        default ResourceType getReloadType() {
            return ResourceType.CLIENT_RESOURCES;
        }
    }

    @FunctionalInterface
    public interface ServerDataReloadEvent extends ReloadEvent {
        @Override
        default ResourceType getReloadType() {
            return ResourceType.SERVER_DATA;
        }
    }
}
