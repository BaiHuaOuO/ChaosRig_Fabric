package dev.chaosrig.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Vec3dHelper {

    public static HitResult getHitResult(@NotNull Entity watcher, float delta, int maxDistance, boolean ignoreInvisible) {
        Vec3d camPos = watcher.getCameraPosVec(delta);
        Vec3d camRotationVec = watcher.getRotationVec(1.0f);
        return getHitResult(watcher, camPos, camRotationVec, maxDistance, ignoreInvisible);
    }

    @Nullable
    public static HitResult getHitResult(@NotNull Entity watcher, Vec3d camPos, Vec3d camRotationVec, int maxDistance, boolean ignoreInvisible) {
        double distance = maxDistance;
        Vec3d camRayEnd = camPos.add(camRotationVec.x * maxDistance, camRotationVec.y * maxDistance, camRotationVec.z * maxDistance);
        BlockHitResult result = watcher.getWorld().raycast(new RaycastContext(camPos, camRayEnd, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, watcher));
        if (result != null) {
            distance = result.getPos().squaredDistanceTo(camPos);
        }
        Box box = watcher.getBoundingBox().stretch(camRotationVec.multiply(maxDistance)).expand(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(watcher, camPos, camRayEnd, box, entity -> !entity.isSpectator(), distance);
        if (entityHitResult != null) {
            Entity target = entityHitResult.getEntity();
            if (watcher.getUuid().equals(target.getUuid())) {
                return result;
            }
            if (ignoreInvisible) {
                return entityHitResult;
            }
            if (watcher instanceof PlayerEntity player && !target.isInvisibleTo(player)) {
                return entityHitResult;
            }
        }
        return result;
    }

    /**
     * 获得两个坐标向量的夹角
     * @param p1 pos1
     * @param p2 pos2
     * @return 夹角(角度制)
     */
    public static double getPosAngle(@NotNull Vec3d p1, @NotNull Vec3d p2) {
        Vec3d norP1 = p1.normalize();
        Vec3d norP2 = p2.normalize();
        double product = norP1.dotProduct(norP2);
        return Math.toDegrees(Math.acos(product));
    }

    @Environment(EnvType.CLIENT)
    public static class ClientHelper {

        /**
         * 获取<code>camera</code>视角上的一条方向向量与目标坐标向量的夹角
         * @param cam 视角cam
         * @param positon 目标位置
         * @return 夹角(角度制)
         */
        public static double getCameraPosAngle(@NotNull Camera cam, @NotNull Vec3d positon) {
            double pitch = Math.toRadians(cam.getPitch());
            double yaw = Math.toRadians(cam.getYaw());
            Vec3d camView = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
            Vec3d camPos = cam.getPos();
            Vec3d toPosVec = positon.subtract(camPos);
            return getPosAngle(camView, toPosVec);
        }

        public static HitResult getScreenRaycast(int reach, boolean ignoreInvisible) {
            MinecraftClient client = MinecraftClient.getInstance();
            Mouse mouse = client.mouse;
            Window window = client.getWindow();
            int mouseX = (int) (mouse.getX() / window.getScaleFactor());
            int mouseY = (int) (mouse.getY() / window.getScaleFactor());
            return getScreenRaycast(mouseX, mouseY, reach, ignoreInvisible);
        }

        public static Vec3d getMousePointAt(int reach) {
            MinecraftClient client = MinecraftClient.getInstance();
            Mouse mouse = client.mouse;
            Window window = client.getWindow();
            int mouseX = (int) (mouse.getX() / window.getScaleFactor());
            int mouseY = (int) (mouse.getY() / window.getScaleFactor());
            return getMousePointAt(mouseX, mouseY, reach);
        }

        public static Vec3d getMousePointAt(int screenX, int screenY, int reach) {
            // from: https://gist.github.com/JXSnack/d19f4caaa05b9e2fbc7bdd9d9b9aa3ab, code edited
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            if (world == null) {
                return null;
            }
            Window window = client.getWindow();
            Camera camera = client.gameRenderer.getCamera();

            int screenWidth = window.getScaledWidth();
            int screenHeight = window.getScaledHeight();
            double fov = client.options.getFov().getValue();
            float yaw = camera.getYaw();
            float pitch = camera.getPitch();
            float ndcX = (2.0f * screenX) / screenWidth - 1.0f;
            float ndcY = 1.0f - (2.0f * screenY) / screenHeight;

            Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), screenWidth / (float) screenHeight, 0.05f, 1000.0f);
            Matrix4f view = new Matrix4f()
                    .rotate((float) Math.toRadians(pitch), new Vector3f(1, 0, 0))
                    .rotate((float) Math.toRadians(yaw + 180), new Vector3f(0, 1, 0));
            Matrix4f invProjectionView = new Matrix4f();
            projection.mul(view, invProjectionView);
            invProjectionView.invert();

            Vector4f rayStart = new Vector4f(ndcX, ndcY, -1, 1.0f);
            Vector4f rayEnd = new Vector4f(ndcX, ndcY, 1, 1.0f);
            rayStart.mul(invProjectionView);
            rayEnd.mul(invProjectionView);
            rayStart.div(rayStart.w);
            rayEnd.div(rayEnd.w);

            Vector3f rayDir = new Vector3f(
                    rayEnd.x - rayStart.x,
                    rayEnd.y - rayStart.y,
                    rayEnd.z - rayStart.z
            ).normalize();

            Vec3d direction = new Vec3d(rayDir.x, rayDir.y, rayDir.z);
            return camera.getPos().add(direction.multiply(reach));
        }

        @Nullable
        public static HitResult getScreenRaycast(int screenX, int screenY, int reach, boolean ignoreInvisible) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            if (world == null) {
                return null;
            }
            Camera camera = client.gameRenderer.getCamera();
            Vec3d end = getMousePointAt(screenX, screenY, reach);
            BlockHitResult blockHitResult = world.raycast(new RaycastContext(camera.getPos(), end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));
            Box box = new Box(camera.getPos(), end).expand(1.0, 1.0, 1.0);
            EntityHitResult entityHitResult = ProjectileUtil.raycast(client.player, camera.getPos(), end, box, entity -> !entity.isSpectator(), reach);
            if (entityHitResult != null) {
                Entity target = entityHitResult.getEntity();
                if (client.player.getUuid().equals(target.getUuid())) {
                    return blockHitResult;
                }
                if (ignoreInvisible) {
                    return entityHitResult;
                }
                if (!target.isInvisibleTo(client.player)) {
                    return entityHitResult;
                }
            }
            return blockHitResult;
        }

    }
}
