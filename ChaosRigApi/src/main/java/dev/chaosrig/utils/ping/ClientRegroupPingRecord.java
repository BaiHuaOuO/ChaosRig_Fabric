package dev.chaosrig.utils.ping;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class ClientRegroupPingRecord extends ClientPingRecord {

    public ClientRegroupPingRecord(@NotNull LivingEntity owner, int maxTick) {
        super(Type.REGROUP, owner, new BlockHitResult(owner.getPos(), Direction.UP, owner.getBlockPos(), false), maxTick);
    }

    @Override
    protected void onRender(@NotNull WorldRenderContext context, Camera camera, MatrixStack matrices, Profiler profiler, double x, double y, double z) {
        float radius = 0.85f;
        int segments = this.getSegments(context, radius, this.getPos());
        this.renderCircleInWorld(matrices, profiler, x, y - 0.5, z, radius, segments, 0.01f);
        radius = 1f;
        segments = this.getSegments(context, radius, this.getPos());
        this.renderCircleInWorld(matrices, profiler, x, y - 0.5, z, radius, segments, 0.04f);
        this.renderFlag(matrices, camera, profiler, x, y - 0.3, z);
    }

    protected void renderFlag(MatrixStack matrices, Camera camera, Profiler profiler, double x, double y, double z) {
        matrices.push();
        profiler.push("rendering flag");
        matrices.translate(x, y, z);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float alpha = this.getAlpha();
        buffer.vertex(matrix4f, 0, 0, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f, 0, 0.48f, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f, 0, 0.45f, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f, 0.3f, 0.35f, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f, 0.3f, 0.35f, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f, 0, 0.25f, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        Tessellator.getInstance().draw();
        matrices.pop();
    }

    protected void renderCircleInWorld(MatrixStack matrices, Profiler profiler, double x, double y, double z, double maxRadius, int segments, double coarse) {
        matrices.push();
        profiler.push("rendering circle");
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        double radius = MathHelper.clamp(0.008 * this.renderProgress * this.renderProgress, 0.1f, maxRadius);
        radius -= coarse / 2f;
        float alpha = this.getAlpha();
        for (int i = 0; i <= segments; i++) {
            for (double j = 0; j < coarse; j += 0.01) {
                double angle = 2 * Math.PI * i / segments;
                float xR = (float) ((radius + j) * Math.cos(angle));
                float yR = (float) ((radius + j) * Math.sin(angle));
                buffer.vertex(matrix4f, xR, yR, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
            }
        }
        Tessellator.getInstance().draw();
        matrices.pop();
    }

    @Override
    public @NotNull Vec3d getPos() {
        return ((BlockHitResult) this.hitResult).getBlockPos().toCenterPos();
    }
}
