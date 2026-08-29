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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Iterator;
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
        renderers.add(new BlockOutlineRenderer(position, maxTick, color));
    }

    public static void removeBlock(@NotNull BlockPos position) {
        renderers.removeIf(r -> r instanceof BlockOutlineRenderer blockOutlineRenderer && blockOutlineRenderer.position.equals(position));
    }

    public static void addEntity(@NotNull Vec3d pos, @NotNull Entity target, int maxTick) {
        renderers.add(new EntityMdoelRenderer(pos, target, maxTick));
    }

    public static void removeEntity(@NotNull Entity entity) {
        renderers.removeIf(r -> r instanceof EntityMdoelRenderer entityMdoelRenderer && entityMdoelRenderer.entity.getUuid().equals(entity.getUuid()));
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

    @Environment(EnvType.CLIENT)
    public static class BlockOutlineRenderer extends Renderer {
        protected final BlockPos position;
        protected final int color;

        public BlockOutlineRenderer(@NotNull BlockPos position, int maxTick, int color) {
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

    @Environment(EnvType.CLIENT)
    public static class EntityMdoelRenderer extends Renderer {
        protected final Entity entity;
        protected final Vec3d pos;
        protected final float yaw;
        public static final ModelPart.Cuboid CUBE = new ModelPart.Cuboid(0, 0, 0, 0, 0, 16, 16, 16, 0, 0, 0, false, 0, 0, EnumSet.allOf(Direction.class));

        public EntityMdoelRenderer(Vec3d pos, Entity entity, int maxTick) {
            super(maxTick);
            this.entity = entity;
            this.pos = pos;
            this.yaw = entity.getYaw();
        }

        @Override
        public void render(WorldRenderContext context, MatrixStack matrices, Profiler profiler) {
            Frustum frustum = context.frustum();
            if (!frustum.isVisible(new Box(BlockPos.ofFloored(pos)))) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            Vec3d camPos = context.camera().getPos();
            VertexConsumerProvider vertexConsumers = context.consumers();
            double x = pos.x - camPos.x;
            double y = pos.y - camPos.y;
            double z = pos.z - camPos.z;
            profiler.push("starting to render");
            matrices.push();
            if (!this.entity.isInvisibleTo(client.player)) {
                matrices.translate(x, y, z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.entity.getYaw() - this.yaw));
                profiler.push("rendering entity");
                EntityRenderDispatcher entityRenderDispatcher = client.getEntityRenderDispatcher();
                EntityRenderer<? super Entity> entityRenderer = entityRenderDispatcher.getRenderer(this.entity);
                entityRenderer.render(this.entity, this.yaw, context.tickDelta(), matrices, layer -> {
                    if (layer.getDrawMode().equals(VertexFormat.DrawMode.QUADS)) {
                        return vertexConsumers.getBuffer(RenderLayer.getTextIntensitySeeThrough(entityRenderer.getTexture(this.entity)));
                    } else {
                        return MinecraftClient.getInstance().getBufferBuilders().getEffectVertexConsumers().getBuffer(layer);
                    }
                }, 15728880);
            } else {
                matrices.translate(x, y - 0.5, z);
                matrices.scale(0.5f, 0.5f, 0.5f);
                profiler.push("rendering cube");
                int alpha = this.maxTick >= 0 ? (int) (MathHelper.clamp(Math.sin((Math.min(1.0f, (double) this.getTick() / this.maxTick) * Math.PI) / 2), 0, 1f) * 255) : 255;
                CUBE.renderCuboid(matrices.peek(), vertexConsumers.getBuffer(RenderLayer.getTextBackgroundSeeThrough()), 15728880, OverlayTexture.DEFAULT_UV, 170, 170, 170, alpha);
            }
            matrices.pop();
        }
    }
 }
