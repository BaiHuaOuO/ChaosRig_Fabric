package dev.chaosrig.utils.ping;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class ClientBlockPingRecord extends ClientPingRecord {
    public final int MAX_RENDER_PROGRESS = 60;

    public ClientBlockPingRecord(@NotNull LivingEntity owner, @NotNull BlockHitResult hitResult, int maxTick) {
        super(Type.BLOCK, owner, hitResult, maxTick);
    }

    @Override
    public @NotNull Vec3d getPos() {
        return ((BlockHitResult) this.hitResult).getBlockPos().toCenterPos();
    }

    @Override
    protected void onTick() {
        super.onTick();
        BlockState blockState = this.owner.getWorld().getBlockState(((BlockHitResult) this.hitResult).getBlockPos());
        if (blockState == null) {
            this.setCancel();
            return;
        }
        if (blockState.isAir()) {
            this.setCancel();
            return;
        }
    }

    @Override
    public void onRender(@NotNull WorldRenderContext context, Camera camera, MatrixStack matrices, Profiler profiler, double x, double y, double z) {
        float size = MathHelper.clamp((float) (Math.sqrt(x * x + y * y + z * z) * 0.028), 0.16f, 1.3f);
        this.renderSide(matrices, camera, profiler, size, x, y, z, 0);
        this.renderSide(matrices, camera, profiler, size, x, y, z, 90);
        this.renderSide(matrices, camera, profiler, size, x, y, z, 180);
        this.renderSide(matrices, camera, profiler, size, x, y, z,  270);
    }

    protected void renderSide(MatrixStack matrices, Camera camera, Profiler profiler, float size, double x, double y, double z, int angle) {
        profiler.push("rendering block ping");
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float alpha = this.getAlpha();
        buffer.vertex(matrix4f, -size, -size / 2, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f, -size, -size, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f, -size, -size, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        buffer.vertex(matrix4f, -size / 2f, -size, 0).color(1.0f, 1.0f, 1.0f, alpha).next();
        Tessellator.getInstance().draw();
        matrices.pop();
    }

}
