package dev.chaosrig.utils.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.chaosrig.ChaosRigApi;
import dev.chaosrig.ChaosRigApiClient;
import dev.chaosrig.utils.config.ClientChaosRigApiConfig;
import dev.chaosrig.utils.renderer.mixin.PostEffectProcessorAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class PostShaders {
    private static int lastTextureWidth = -1;
    private static int lastTextureHeight = -1;

    @Nullable
    public static PostEffectProcessor vhs;
    private static boolean enableVhs = false;

    public static void register() {
        if (ChaosRigApiClient.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        WorldRenderEvents.LAST.register(PostShaders::render);
        ClientLifecycleEvents.CLIENT_STARTED.register(PostShaders::init);
    }

    public static void init(MinecraftClient client) { // 预留mixin空间
        reload(client, client.getResourceManager());
    }

    public static void render(WorldRenderContext context) {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (framebuffer.textureWidth != lastTextureWidth || framebuffer.textureHeight != lastTextureHeight) {
            lastTextureWidth = framebuffer.textureWidth;
            lastTextureHeight = framebuffer.textureHeight;
            reset();
        }
        onVhsRender(context);
    }

    public static boolean isEnableVhs() {
        return enableVhs;
    }

    public static void toggleVhs() {
        enableVhs = !enableVhs;
    }

    public static void setVhs(boolean value) {
        enableVhs = value;
    }

    public static void reload(MinecraftClient client, ResourceManager resourceManager) {
        try {
            Framebuffer framebuffer = client.getFramebuffer();
            TextureManager textureManager = client.getTextureManager();
            if (vhs != null) {
                vhs.close();
                vhs = null;
            }
            vhs = new PostEffectProcessor(
                    textureManager,
                    resourceManager,
                    framebuffer,
                    new Identifier(ChaosRigApi.API_MOD_ID, "shaders/post/vhs_overlay.json")
            );
            resetVhsUniform();
            int width = framebuffer.textureWidth;
            int height = framebuffer.textureHeight;
            lastTextureWidth = width;
            lastTextureHeight = height;
            reset();
        } catch (IOException e) {
            ChaosRigApi.LOGGER.error("无法加载shader: ", e);
        }
    }

    public static void resetVhsUniform() {
        PostShaders.getPasses(PostShaders.vhs).stream().filter(p -> p.getName().equals("vhs_program")).forEach(e -> {
            JsonEffectShaderProgram program = e.getProgram();
            program.getUniformByName("BarrelAmount").set(ClientChaosRigApiConfig.vhsBarrelAmount);
            program.getUniformByName("ChromaAberration").set(ClientChaosRigApiConfig.vhsChromaAberration);
            program.getUniformByName("ChromaEdge").set(ClientChaosRigApiConfig.vhsChromaEdge);
            program.getUniformByName("ChromaSmear").set(ClientChaosRigApiConfig.vhsChromaSmear);
            program.getUniformByName("TrackSpeed").set(ClientChaosRigApiConfig.vhsTrackSpeed);
            program.getUniformByName("TrackWidth").set(ClientChaosRigApiConfig.vhsTrackWidth);
            program.getUniformByName("TrackJitter").set(ClientChaosRigApiConfig.vhsTrackJitter);
            program.getUniformByName("TrackBright").set(ClientChaosRigApiConfig.vhsTrackBright);
            program.getUniformByName("FlickerAmount").set(ClientChaosRigApiConfig.vhsFlickerAmount);
            program.getUniformByName("ScanlineStrength").set(ClientChaosRigApiConfig.vhsScanlineStrength);
            program.getUniformByName("GrainStrength").set(ClientChaosRigApiConfig.vhsGrainStrength);
            program.getUniformByName("VignetteStrength").set(ClientChaosRigApiConfig.vhsVignetteStrength);
            program.getUniformByName("OffsetIntensity").set(ClientChaosRigApiConfig.vhsOffsetIntensity);
            program.getUniformByName("NoiseIntensity").set(ClientChaosRigApiConfig.vhsNoiseIntensity);
            program.getUniformByName("BlurNear").set(ClientChaosRigApiConfig.vhsBlurNear);
            program.getUniformByName("BlurFar").set(ClientChaosRigApiConfig.vhsBlurFar);
            program.getUniformByName("BlurRadius").set(ClientChaosRigApiConfig.vhsBlurRadius);
            program.getUniformByName("BlurSamples").set(ClientChaosRigApiConfig.vhsBlurSamples);
            program.getUniformByName("MaxColorBlur").set(ClientChaosRigApiConfig.vhsMaxColorBlur);
        });
    }

    private static void reset() {
        if (vhs != null) {
            vhs.setupDimensions(lastTextureWidth, lastTextureHeight);
        }
    }

    private static List<PostEffectPass> getPasses(PostEffectProcessor postEffectProcessor) {
        return ((PostEffectProcessorAccessor) postEffectProcessor).getPasses();
    }

    public static void onVhsRender(WorldRenderContext context) {
        if (!enableVhs) {
            return;
        }
        if (vhs == null) {
            return;
        }
        RenderSystem.enableBlend();
        if (context.world().getTime() % 20 == 0) {
            resetVhsUniform();
        }
        vhs.render(context.tickDelta());
        RenderSystem.disableBlend();
    }
}
