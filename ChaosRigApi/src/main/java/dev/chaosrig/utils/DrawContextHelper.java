package dev.chaosrig.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class DrawContextHelper {

    /**
     * <p>绘画一个有过渡颜色的实心圆</p>
     * <p>
     *     从圆心建立x, y直角坐标系中的x轴加上<code>startAngle</code>的偏转角度开始绘画, 方向为顺时针方向
     * </p>
     * @param segments 线段数; 分割数
     * @param centerColor 圆心颜色
     * @param outlineColor 从圆心颜色过渡到边界的颜色
     * @param radius 圆的半径
     * @param x 屏幕x轴位置
     * @param y 屏幕y轴位置
     * @param z z轴图层
     * @param maxAngle 绘画最大角度: <code>360</code>为圆, <code>180</code>为半圆, <code>120</code>为扇形等
     * @param startAngle 开始绘画偏移角
     */
    public static void fillGradientCircle(@NotNull DrawContext context, int segments, int centerColor, int outlineColor, double radius, double x, double y, int z, double maxAngle, double startAngle) {
        if (radius <= 0) throw new IllegalArgumentException("不允许半径传参为负数或为零");
        context.getMatrices().push();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(x, y, z).color(centerColor).next();
        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i / segments) + Math.toRadians(startAngle);
            if (angle > Math.toRadians(maxAngle + startAngle)) break;
            float xR = (float) (x + (radius * Math.cos(angle)));
            float yR = (float) (y + (radius * Math.sin(angle)));
            bufferBuilder.vertex(xR, yR, z).color(outlineColor).next();
        }
        Tessellator.getInstance().draw();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        context.getMatrices().pop();
    }

    /**
     * <p>绘画一个实心圆</p>
     * <p>
     *     从圆心建立x, y直角坐标系中的x轴加上<code>startAngle</code>的偏转角度开始绘画, 方向为顺时针方向
     * </p>
     * @param segments 线段数; 分割数
     * @param color 填充颜色
     * @param radius 圆的半径
     * @param x 屏幕x轴位置
     * @param y 屏幕y轴位置
     * @param z z轴图层
     * @param maxAngle 绘画最大角度: <code>360</code>为圆, <code>180</code>为半圆, <code>120</code>为扇形等
     * @param startAngle 开始绘画偏移角
     */
    public static void fillCircle(@NotNull DrawContext context, int segments, int color, double radius, double x, double y, int z, double maxAngle, double startAngle) {
        fillGradientCircle(context, segments, color, color, radius, x, y, z, maxAngle, startAngle);
    }

    /**
     * <p>绘画一个圆的轮廓</p>
     * <p>
     *      从圆心建立x, y直角坐标系中的x轴加上<code>startAngle</code>的偏转角度开始绘画, 方向为顺时针方向
     * </p>
     * @param segments 线段数; 分割数
     * @param color 颜色
     * @param radius 圆的半径
     * @param coarse 粗细程度
     * @param x 屏幕x轴位置
     * @param y 屏幕y轴位置
     * @param z z轴图层
     * @param maxAngle 绘画最大角度: <code>360</code>为圆轮廓, <code>180</code>为半圆轮廓(不包括直径), <code>120</code>为扇形(不包括直径)等
     * @param startAngle 开始绘画偏移角
     */
    public static void drawCircleOutline(@NotNull DrawContext context, int segments, int color, double radius, int coarse, double x, double y, int z, double maxAngle, double startAngle) {
        if (radius <= 0 || coarse < 1) throw new IllegalArgumentException("不允许半径或粗细大小传参为负数或为零");
        context.getMatrices().push();
        Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        radius -= coarse / 2f;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments + Math.toRadians(startAngle);
            for (int j = 0; j < coarse; j++) {
                if (angle >= Math.toRadians(maxAngle + startAngle)) break;
                float xR = (float) (x + ((radius + j) * Math.cos(angle)));
                float yR = (float) (y + ((radius + j) * Math.sin(angle)));
                bufferBuilder.vertex(matrix4f, xR, yR, z).color(color).next();
            }
        }
        Tessellator.getInstance().draw();
        context.getMatrices().pop();
    }

    /**
     * <p>绘画带有横向过渡颜色的矩形</p>
     * <p>该方法为{@link DrawContext#fillGradient(int, int, int, int, int, int)}部分重载, 旨在横向而不是纵向过渡颜色</p>
     * @param startX 起始x轴坐标
     * @param startY 起始y轴坐标
     * @param endX 末端x轴坐标
     * @param endY 末端y轴坐标
     * @param colorStart 左边填充颜色
     * @param colorEnd 右边填充颜色
     */
    public static void fillGradientHorizontal(@NotNull DrawContext context, int startX, int startY, int endX, int endY, int colorStart, int colorEnd) {
        float alphaStart = (float) ColorHelper.Argb.getAlpha(colorStart) / 255.0f;
        float redStart = (float) ColorHelper.Argb.getRed(colorStart) / 255.0f;
        float greenStart = (float) ColorHelper.Argb.getGreen(colorStart) / 255.0f;
        float blueStart = (float) ColorHelper.Argb.getBlue(colorStart) / 255.0f;
        float alphaEnd = (float) ColorHelper.Argb.getAlpha(colorEnd) / 255.0f;
        float redEnd = (float) ColorHelper.Argb.getRed(colorEnd) / 255.0f;
        float greenEnd = (float) ColorHelper.Argb.getGreen(colorEnd) / 255.0f;
        float blueEnd = (float) ColorHelper.Argb.getBlue(colorEnd) / 255.0f;
        RenderSystem.enableBlend();
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, startX, startY, 0).color(redStart, greenStart, blueStart, alphaStart).next();
        bufferBuilder.vertex(matrix4f, endX, startY, 0).color(redEnd, greenEnd, blueEnd, alphaEnd).next();
        bufferBuilder.vertex(matrix4f, endX, endY, 0).color(redEnd, greenEnd, blueEnd, alphaEnd).next();
        bufferBuilder.vertex(matrix4f, startX, endY, 0).color(redStart, greenStart, blueStart, alphaStart).next();
        Tessellator.getInstance().draw();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    /**
     * <p>绘画带有对角线过渡颜色的矩形</p>
     * <p>该方法为{@link DrawContext#fillGradient(int, int, int, int, int, int)}部分重载</p>
     * @param startX 起始x轴坐标
     * @param startY 起始y轴坐标
     * @param endX 末端x轴坐标
     * @param endY 末端y轴坐标
     * @param colorStart 右上角填充颜色
     * @param colorEnd 左下角填充颜色
     */
    public static void fillGradientDiagonal(@NotNull DrawContext context, int startX, int startY, int endX, int endY, int colorStart, int colorEnd) {
        float alphaStart = (float) ColorHelper.Argb.getAlpha(colorStart) / 255.0f;
        float redStart = (float) ColorHelper.Argb.getRed(colorStart) / 255.0f;
        float greenStart = (float) ColorHelper.Argb.getGreen(colorStart) / 255.0f;
        float blueStart = (float) ColorHelper.Argb.getBlue(colorStart) / 255.0f;
        float alphaEnd = (float) ColorHelper.Argb.getAlpha(colorEnd) / 255.0f;
        float redEnd = (float) ColorHelper.Argb.getRed(colorEnd) / 255.0f;
        float greenEnd = (float) ColorHelper.Argb.getGreen(colorEnd) / 255.0f;
        float blueEnd = (float) ColorHelper.Argb.getBlue(colorEnd) / 255.0f;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, startX, endY, 0).color(redStart, greenStart, blueStart, alphaStart).next();
        bufferBuilder.vertex(matrix4f, endX, endY, 0).color(redEnd, greenEnd, blueEnd, alphaEnd).next();
        bufferBuilder.vertex(matrix4f, endX, startY, 0).color(redEnd, greenEnd, blueEnd, alphaEnd).next();
        bufferBuilder.vertex(matrix4f, startX, startY, 0).color(redEnd, greenEnd, blueEnd, alphaEnd).next();
        Tessellator.getInstance().draw();
        RenderSystem.enableCull();
        matrices.pop();
    }

    public record Outline(int leftTop, int leftBottom, int rightTop, int rightBottom) {}
}
