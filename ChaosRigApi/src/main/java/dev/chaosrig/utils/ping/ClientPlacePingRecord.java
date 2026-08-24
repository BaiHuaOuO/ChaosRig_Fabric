package dev.chaosrig.utils.ping;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.chaosrig.utils.ColorTools;
import dev.chaosrig.utils.renderer.InformationScreen;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ClientPlacePingRecord extends ClientPingRecord {
    // IMPORTANT: 更改此值必须将一些硬编码值更改
    public final int MAX_RENDER_PROGRESS = 60;

    public ClientPlacePingRecord(@NotNull LivingEntity owner, @NotNull BlockHitResult hitResult, int maxTick) {
        super(Type.LOCATION, owner, hitResult, maxTick);
    }

    @Override
    @NotNull
    public BlockHitResult getHitResult() {
        return (BlockHitResult) super.getHitResult();
    }

    @Override
    public boolean isCanceled() {
        return super.isCanceled() && renderProgress >= MAX_RENDER_PROGRESS;
    }

    @Override
    public void onRender(@NotNull WorldRenderContext context, Camera camera, MatrixStack matrices, Profiler profiler, double x, double y, double z) {
        float size = MathHelper.clamp((float) (Math.sqrt(x * x + y * y + z * z) * 0.028), 0.08f, 1.6f);
        this.renderX(matrices, camera, profiler, size, x, y, z);
        int segments = this.getSegments(context, size * 10, this.getPos());
        InformationScreen.push("segments", -1, ColorTools.WHITE.apply(255), () -> String.valueOf(segments));
        this.renderCircleInWorld(matrices, profiler, x, y, z, segments, 0.05f);
    }

    protected void renderCircleInWorld(MatrixStack matrices, Profiler profiler, double x, double y, double z, int segments, double coarse) {
        matrices.push();
        profiler.push("rendering circle");
        Vector3f directionVec = this.getHitResult().getSide().getUnitVector();
        Vector3f upVec = new Vector3f(0, 1.0f, 0.0f);
        if (this.getHitResult().getSide().getAxis() == Direction.Axis.Y) {
            upVec = new Vector3f(0, 0, 1.0f);
        }
        Quaternionf rotation = new Quaternionf().lookAlong(directionVec, upVec);
        matrices.translate(x, y, z);
        matrices.multiply(rotation);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        double radius = MathHelper.clamp(0.008 * this.renderProgress * this.renderProgress, 0.1f, 1.2f);
        radius -= coarse / 2f;
        float alpha = MathHelper.clamp(this.renderProgress / 20f, 0, 1) - MathHelper.clamp((this.renderProgress - 40) / 20f, 0, 1);
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

    protected void renderX(MatrixStack matrices, Camera camera, Profiler profiler, float size, double x, double y, double z) {
        matrices.push();
        profiler.push("rendering X");
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.renderProgress * 6));
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        // draw X
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float lineSize = size + MathHelper.clamp((40 - this.renderProgress) * 0.2f, 0, 1.2f); // +?
        float alpha = Math.min(this.getRenderAlpha(), maxTick >= MAX_RENDER_PROGRESS
                ? MathHelper.clamp(this.tick / 10f, 0, 1) - MathHelper.clamp((this.tick - maxTick - 10) / 10f, 0, 1)
                : MathHelper.clamp(this.renderProgress / 10f, 0, 1) - MathHelper.clamp((this.renderProgress - 50) / 10f, 0, 1));
        buffer.vertex(matrix4f, -lineSize, -lineSize, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f,  lineSize,  lineSize, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f,  lineSize, -lineSize, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f, -lineSize,  lineSize, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        Tessellator.getInstance().draw();
        // draw circle in X
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        double radius = lineSize / 1.2f;
        for (int i = 0; i <= 100; i++) {
            double angle = 2 * Math.PI * i / 100;
            float xR = (float) (radius * Math.cos(angle));
            float yR = (float) (radius * Math.sin(angle));
            buffer.vertex(matrix4f, xR, yR, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        }
        Tessellator.getInstance().draw();
        matrices.pop();
    }
}
