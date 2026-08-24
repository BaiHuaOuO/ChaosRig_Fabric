package dev.chaosrig.utils;

import net.minecraft.util.math.ColorHelper;

import java.util.function.Function;

public class ColorTools {
    /**
     * <p>纯白色(255, 255, 255, a), a由apply();决定</p>
     * <p>a∈[0, 255]</p>
     */
    public static Function<Integer, Integer> WHITE = alpha -> ColorHelper.Argb.getArgb(alpha >= 0 && alpha <= 255 ? alpha : 255, 255, 255, 255);
    /**
     * <p>纯红色(255, 0, 0, a), a由apply();决定</p>
     * <p>a∈[0, 255]</p>
     */
    public static Function<Integer, Integer> RED = alpha -> ColorHelper.Argb.getArgb(alpha >= 0 && alpha <= 255 ? alpha : 255, 255, 0, 0);
    /**
     * <p>纯黄色(255, 255, 0, a), a由apply();决定</p>
     * <p>a∈[0, 255]</p>
     */
    public static Function<Integer, Integer> YELLOW = alpha -> ColorHelper.Argb.getArgb(alpha >= 0 && alpha <= 255 ? alpha : 255, 255, 255, 0);
    /**
     * <p>纯黑色(0, 0, 0, a), a由apply();决定</p>
     * <p>a∈[0, 255]</p>
     */
    public static Function<Integer, Integer> BLACK = alpha -> ColorHelper.Argb.getArgb(alpha >= 0 && alpha <= 255 ? alpha : 255, 0, 0, 0);

    public static Function<Integer, Integer> LIGHT_RED = alpha -> ColorHelper.Argb.getArgb(alpha >= 0 && alpha <= 255 ? alpha : 255, 255, 107, 107);
    public static Function<Integer, Integer> LIME = alpha -> ColorHelper.Argb.getArgb(alpha >= 0 && alpha <= 255 ? alpha : 255, 50, 205, 50);
    public static Function<Integer, Integer> GRAY = alpha -> ColorHelper.Argb.getArgb(alpha >= 0 && alpha <= 255 ? alpha : 255, 128, 128, 128);

    /**
     * Minecraft {@link ColorHelper.Argb#getArgb(int, int, int, int)}的包装方法
     * @param alpha 透明度
     * @param red R区间
     * @param blue B区间
     * @param green G区间
     * @return argb数值
     */
    public static int getColor(int alpha, int red, int blue, int green) {
        return ColorHelper.Argb.getArgb(alpha, red, blue, green);
    }

    /**
     * hex字符转换为R区间
     * @param hex 16进制类型的字符串
     * @return R通道参数[0, 255]
     * @throws IllegalArgumentException 传参形式不正确
     */
    public static int hexToRed(String hex) {
        if (hex.isEmpty()) return 255;
        String hexText = hexToText(hex);
        return Integer.parseInt(hexText.substring(2, 4), 16);
    }

    /**
     * hex字符转换为G区间
     * @param hex 16进制类型的字符串
     * @return G通道参数[0, 255]
     * @throws IllegalArgumentException 传参形式不正确
     */
    public static int hexToGreen(String hex) {
        if (hex.isEmpty()) return 255;
        String hexText = hexToText(hex);
        return Integer.parseInt(hexText.substring(4, 6), 16);
    }

    /**
     * hex字符转换为B区间
     * @param hex 16进制类型的字符串
     * @return B通道参数[0, 255]
     * @throws IllegalArgumentException 传参形式不正确
     */
    public static int hexToBlue(String hex) {
        if (hex.isEmpty()) return 255;
        String hexText = hexToText(hex);
        return Integer.parseInt(hexText.substring(6, 8), 16);
    }

    /**
     * hex字符转换为A区间
     * @param hex 16进制类型的字符串
     * @return A通道参数[0, 255]
     * @throws IllegalArgumentException 传参形式不正确
     */
    public static int hexToAlpha(String hex) {
        if (hex.isEmpty()) return 255;
        String hexText = hexToText(hex);
        return Integer.parseInt(hexText.substring(0, 2), 16);
    }

    protected static String hexToText(String hex) {
        String hexColor = hex.startsWith("#") ? hex.substring(1) : hex;
        if (hexColor.length() == 3 || hexColor.length() == 4) {
            StringBuilder sb = new StringBuilder();
            for (char c : hexColor.toCharArray()) {
                sb.append(c).append(c);
            }
            hexColor = sb.toString();
        }
        if (hexColor.length() == 6) {
            hexColor = "FF" + hexColor;
        }
        if (hexColor.length() != 8) throw new IllegalArgumentException("传参Hex字符串格式不正确");
        return hexColor;
    }
}
