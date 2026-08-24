package dev.chaosrig.utils;

import dev.chaosrig.ChaosRigApi;
import dev.chaosrig.ChaosRigApiClient;
import dev.chaosrig.utils.renderer.PostShaders;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

    public static void register() {
        if (ChaosRigApiClient.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        registerReload();
    }

    protected static void registerReload() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(ChaosRigApi.API_MOD_ID, "json_reload");
            }

            @Override
            public void reload(ResourceManager manager) {

            }
        });
    }

    /**
     * <p>临时获取一个文件实例</p>
     * <p>禁止频繁调用</p>
     * @param modResource 目标路径
     * @return {@link Resource}对象, 文件不存在为<code>Optional.empty()</code>
     */
    @NotNull
    public static Optional<Resource> getResource(@NotNull Identifier modResource) {
        return MinecraftClient.getInstance().getResourceManager().getResource(modResource);
    }

    /**
     * <p>临时获取一个文件的输入流</p>
     * <p>禁止频繁调用</p>
     * @param modResource 目标路径
     * @return {@link InputStream}对象, 文件不存在为<code>Optional.empty()</code>
     */
    @NotNull
    public static Optional<InputStream> getInputStream(@NotNull Identifier modResource) {
        return getResource(modResource).flatMap(r -> {
            try {
                return Optional.of(r.getInputStream());
            } catch (IOException e) {
                ChaosRigApi.LOGGER.error("对象:\"{}\"的InputStream获取失败: {}", modResource, e);
                return Optional.empty();
            }
        });
    }

    /**
     * <p>临时获取一个文件的缓存输出流</p>
     * <p>禁止频繁调用</p>
     * @param modResource 目标路径
     * @return {@link BufferedReader}对象, 文件不存在为<code>Optional.empty()</code>
     */
    @NotNull
    public static Optional<BufferedReader> getReader(@NotNull Identifier modResource) {
        return getResource(modResource).flatMap(r -> {
            try {
                return Optional.of(r.getReader());
            } catch (IOException e) {
                ChaosRigApi.LOGGER.error("对象:\"{}\"的BufferedReader获取失败: {}", modResource, e);
                return Optional.empty();
            }
        });
    }
}
