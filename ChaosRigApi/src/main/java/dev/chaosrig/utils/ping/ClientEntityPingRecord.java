package dev.chaosrig.utils.ping;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.chaosrig.utils.ColorTools;
import dev.chaosrig.utils.renderer.InformationScreen;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class ClientEntityPingRecord extends ClientPingRecord {
    public final int MAX_RENDER_PROGRESS = 60;

    public ClientEntityPingRecord(@NotNull LivingEntity owner, @NotNull EntityHitResult hitResult, int maxTick) {
        super(Type.ENTITY, owner, hitResult, maxTick);
    }

    @Override
    public @NotNull Vec3d getPos() {
        return ((EntityHitResult) this.hitResult).getEntity().getEyePos();
    }

    @Override
    public void onRender(@NotNull WorldRenderContext context, Camera camera, MatrixStack matrices, Profiler profiler, double x, double y, double z) {
        float size = Math.min(MathHelper.clamp((float) (Math.sqrt(x * x + y * y + z * z) * 0.32), 0.08f, 1.6f), MathHelper.clamp(this.renderProgress * 0.05f, 0.2f, 0.8f));
        int segments = this.getSegments(context, size, this.getPos());
        this.renderCircle(matrices, camera, profiler, x, y, z, size, segments, 0.03f, 70, 5, true);
        this.renderCircle(matrices, camera, profiler, x, y, z, size, segments, 0.03f, 70, 95, true);
        this.renderCircle(matrices, camera, profiler, x, y, z, size, segments, 0.03f, 70, 185, true);
        this.renderCircle(matrices, camera, profiler, x, y, z, size, segments, 0.03f, 70, 275, true);
        this.renderCircle(matrices, camera, profiler, x, y, z, size - 0.15f, segments, 0.01f, 174, 3, false);
        this.renderCircle(matrices, camera, profiler, x, y, z, size - 0.15f, segments, 0.01f, 174, 183, false);
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (((EntityHitResult) this.hitResult).getEntity() instanceof LivingEntity livingEntity) {
            if (livingEntity.isDead()) {
                this.setCancel();
            }
        }
        if (((EntityHitResult) this.hitResult).getEntity().isInvisible()) {
            this.setCancel();
        }
    }

    @Override
    public boolean isCanceled() {
        return super.isCanceled() && this.tick >= MAX_RENDER_PROGRESS;
    }

    public void onUnfocusing() {
        if (renderAlpha <= 0.800f) {
            renderAlpha += 0.05f;
        }
    }

    protected void renderCircle(MatrixStack matrices,
                                Camera camera,
                                Profiler profiler,
                                double x,
                                double y,
                                double z,
                                double radius,
                                int segments,
                                double coarse,
                                double maxAngle,
                                double startAngle,
                                boolean clockwise) {
        matrices.push();
        profiler.push("rendering circle");
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.renderProgress * 6 * (clockwise ? 1 : -1)));
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        float alpha = Math.min(this.getRenderAlpha(), maxTick >= MAX_RENDER_PROGRESS
                ? MathHelper.clamp(this.tick / 10f, 0, 1) - MathHelper.clamp((this.tick - maxTick + 20) / 10f, 0, 1)
                : MathHelper.clamp(this.renderProgress / 10f, 0, 1) - MathHelper.clamp((this.renderProgress - 50) / 10f, 0, 1));
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments + Math.toRadians(startAngle);
            for (double j = 0; j < coarse; j += 0.01) {
                if (angle >= Math.toRadians(maxAngle + startAngle)) break;
                float xR = (float) ((radius + j) * Math.cos(angle));
                float yR = (float) ((radius + j) * Math.sin(angle));
                buffer.vertex(matrix4f, xR, yR, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
            }
        }
        Tessellator.getInstance().draw();
        matrices.pop();
    }

}
