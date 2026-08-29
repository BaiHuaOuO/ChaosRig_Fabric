package dev.chaosrig.utils.ping;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public abstract class ClientPingRecord extends PingRecord {
    public final int MAX_RENDER_PROGRESS = 60;
    protected int renderProgress = 0;
    protected float renderAlpha = 1f;

    public static void copy(ClientPingRecord from, ClientPingRecord to) {
        PingRecord.copy(from, to);
        to.renderProgress = from.renderProgress;
        to.renderAlpha = from.renderAlpha;
    }

    public ClientPingRecord(@NotNull Type type, @NotNull LivingEntity owner, @NotNull HitResult hitResult, int maxTick) {
        super(type, owner, hitResult, maxTick);
    }

    public float getAlpha() {
        return Math.min(this.getRenderAlpha(), maxTick >= MAX_RENDER_PROGRESS
                ? MathHelper.clamp(this.tick / 10f, 0, 1) - MathHelper.clamp((this.tick - maxTick + 20) / 10f, 0, 1)
                : MathHelper.clamp(this.renderProgress / 10f, 0, 1) - MathHelper.clamp((this.renderProgress - 50) / 10f, 0, 1));
    }

    public void render(@NotNull WorldRenderContext context) {
        if (this.isCanceled()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ClientWorld world = context.world();
        if (!world.getWorldBorder().contains(this.getPos().x, this.getPos().y, this.getPos().z)) {
            return;
        }
        Camera camera = context.camera();
        Vec3d pingPos = this.getPos();
        MatrixStack matrices = context.matrixStack();
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
        profiler.push("rendering_place_ping");
        double x = pingPos.x - camera.getPos().x;
        double y = pingPos.y - camera.getPos().y;
        double z = pingPos.z - camera.getPos().z;
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        this.onRender(context, camera, matrices, profiler, x, y, z);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        profiler.pop();
    }

    protected int getSegments(WorldRenderContext context, float radius, Vec3d pingPos) {
        Vec3d camPos = context.camera().getPos();
        double distance = camPos.distanceTo(pingPos);
        Window window = MinecraftClient.getInstance().getWindow();
        double size = Math.max(window.getWidth(), window.getHeight()) * window.getScaleFactor() * 2;
        double baseSegments = (radius * size) / (distance + 0.1);
        return (int) MathHelper.clamp(baseSegments, 50, 1500);
    }

    protected abstract void onRender(@NotNull WorldRenderContext context, Camera camera, MatrixStack matrices, Profiler profiler, double x, double y, double z);

    public void onFocusing() {
        if (renderAlpha >= 0.350) {
            renderAlpha -= 0.05f;
        }
    }

    public void onUnfocusing() {
        if (renderAlpha <= 0.950f) {
            renderAlpha += 0.05f;
        }
    }

    public float getRenderAlpha() {
        return this.renderAlpha;
    }

    @Override
    protected void onTick() {
        this.renderProgress++;
    }

}
