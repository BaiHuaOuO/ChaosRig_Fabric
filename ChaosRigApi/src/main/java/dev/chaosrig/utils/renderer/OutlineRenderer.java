package dev.chaosrig.utils.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.chaosrig.ChaosRigApiClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Environment(EnvType.CLIENT)
public class OutlineRenderer {
    protected static final Queue<Renderer> renderers = new ConcurrentLinkedQueue<>();

    public static void register() {
        if (ChaosRigApiClient.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
        WorldRenderEvents.LAST.register(OutlineRenderer::render);
        ClientTickEvents.END_WORLD_TICK.register(OutlineRenderer::tick);
    }

    public static void addBlock(@NotNull BlockPos position, int color, int maxTick) {
        renderers.add(new BlockRenderer(position, maxTick, color));
    }

    public static void removeBlock(@NotNull BlockPos position) {
        renderers.removeIf(r -> r instanceof BlockRenderer blockRenderer && blockRenderer.position.equals(position));
    }

    protected static void render(WorldRenderContext context) {
        Profiler profiler = context.profiler();
        MatrixStack matrices = context.matrixStack();
        profiler.push("starting render outline");
        for (Renderer renderer : renderers) {
            renderer.render(context, matrices, profiler);
        }
    }

    protected static void tick(ClientWorld world) {
        Iterator<Renderer> rendererIterator = renderers.iterator();
        while (rendererIterator.hasNext()) {
            Renderer renderer = rendererIterator.next();
            if (renderer.isFinish()) {
                rendererIterator.remove();
            }
            renderer.tick();
        }
    }

    @Environment(EnvType.CLIENT)
    public abstract static class Renderer {
        private short tick = 0;
        public final int maxTick;

        protected Renderer(int maxTick) {
            if (maxTick < 0) {
                maxTick = -1;
            }
            this.maxTick = maxTick;
        }

        public abstract void render(WorldRenderContext context, MatrixStack matrices, Profiler profiler);

        public void tick() {
            if (maxTick > 0 && tick < maxTick) {
                tick++;
            }
        }

        public short getTick() {
            return tick;
        }

        public boolean isFinish() {
            return maxTick > 0 && tick >= maxTick;
        }
    }

    public static class BlockRenderer extends Renderer {
        protected final BlockPos position;
        protected final int color;

        protected BlockRenderer(@NotNull BlockPos position, int maxTick, int color) {
            super(maxTick);
            this.position = position;
            this.color = color;
        }

        @NotNull
        public BlockPos getPosition() {
            return this.position;
        }

        @Override
        public void render(WorldRenderContext context, MatrixStack matrices, Profiler profiler) {
            profiler.push("preparing outline renderer needs");
            Camera camera = context.camera();
            Frustum frustum = context.frustum();
            Box posBox = new Box(position, position).expand(0.1f);
            if (!frustum.isVisible(posBox)) {
                return;
            }
            ClientWorld world = context.world();
            BlockState blockState = world.getBlockState(this.position);
            BufferBuilder builder = Tessellator.getInstance().getBuffer();
            VoxelShape voxelShape = blockState.isAir() ? VoxelShapes.fullCube() : blockState.getOutlineShape(world, position, ShapeContext.of(camera.getFocusedEntity()));
            builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            MatrixStack.Entry matricesEntry = matrices.peek();
            double x = (double) position.getX() - camera.getPos().x;
            double y = (double) position.getY() - camera.getPos().y;
            double z = (double) position.getZ() - camera.getPos().z;
            int red = ColorHelper.Argb.getRed(color);
            int blue = ColorHelper.Argb.getBlue(color);
            int green = ColorHelper.Argb.getGreen(color);
            int alpha = ColorHelper.Argb.getAlpha(color);
            matrices.push();
            profiler.push("start to render");
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
                float vx = (float) (maxX - minX);
                float vy = (float) (maxY - minY);
                float vz = (float) (maxZ - minZ);
                float length = MathHelper.sqrt(vx * vx + vy * vy + vz * vz);
                builder.vertex(matricesEntry.getPositionMatrix(), (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                        .normal(matricesEntry.getNormalMatrix(), vx / length, vy / length, vz / length)
                        .color(red, green, blue, alpha)
                        .next();
                builder.vertex(matricesEntry.getPositionMatrix(), (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                        .normal(matricesEntry.getNormalMatrix(), vx, vy, vz)
                        .color(red, green, blue, alpha)
                        .next();
            });
            Tessellator.getInstance().draw();
            matrices.pop();
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.enableCull();
        }
    }
}
