package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.ClientHudLayout.Elements;

/**
 * HUD 元素几何：编辑器预览与实战覆盖层共用同一套位置换算，保证“所见即所得”。
 * 每个元素（比分记录条、状态文字、击杀播报、场景横幅）都按自己的百分比独立定位。
 * 返回的都是屏幕实际像素矩形。
 */
public final class HudGeometry {
    public static final int SCREEN_MARGIN = 8;
    public static final int SCORE_BASE_HEIGHT = 70;
    public static final int TEXT_BASE_HEIGHT = 24;
    public static final int FEED_BASE_HEIGHT = 20;
    public static final int BANNER_BASE_HEIGHT = 34;
    public static final int HOTBAR_SAFE_MARGIN = 54;

    /** 一个已按缩放换算好的屏幕矩形。 */
    public record Rect(int left, int top, int width, int height, float scale,
                       int baseWidth, int baseHeight) {
        public int right() {
            return left + width;
        }

        public int bottom() {
            return top + height;
        }

        public int centerX() {
            return left + width / 2;
        }

        public int centerY() {
            return top + height / 2;
        }

        public boolean contains(double x, double y) {
            return x >= left && x <= right() && y >= top && y <= bottom();
        }
    }

    private HudGeometry() {
    }

    /** 顶部比分记录条：x/y 为面板中心 / 顶边所在的屏幕百分比。 */
    public static Rect score(Elements elements, int screenWidth, int screenHeight) {
        int baseWidth = Math.max(220, elements.scoreWidth());
        float scale = fitScale(elements.scoreScalePercent() / 100.0F, baseWidth, SCORE_BASE_HEIGHT,
                screenWidth - SCREEN_MARGIN * 2, Math.max(1, screenHeight - SCREEN_MARGIN * 2));
        int width = scaled(baseWidth, scale);
        int height = scaled(SCORE_BASE_HEIGHT, scale);
        int centerX = centered(screenWidth, elements.scoreXPercent(), width,
                SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - SCREEN_MARGIN));
        int top = clampedTop(screenHeight, elements.scoreYPercent(), height,
                SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenHeight - SCREEN_MARGIN));
        return new Rect(centerX - width / 2, top, width, height, scale, baseWidth, SCORE_BASE_HEIGHT);
    }

    /** 状态文字条（原底部提示）：独立定位，中心锚点。 */
    public static Rect text(Elements elements, int screenWidth, int screenHeight, int minTextWidth) {
        int baseWidth = Math.max(Math.max(180, elements.textWidth()), minTextWidth);
        return anchored(baseWidth, TEXT_BASE_HEIGHT, elements.textScalePercent(),
                elements.textOpacityPercent(), elements.textXPercent(), elements.textYPercent(),
                screenWidth, screenHeight);
    }

    /** 击杀播报：宽度随文字自适应，中心锚点。 */
    public static Rect feed(Elements elements, int screenWidth, int screenHeight, int minTextWidth) {
        return anchored(Math.max(180, Math.min(420, minTextWidth)), FEED_BASE_HEIGHT, elements.feedScalePercent(),
                elements.feedOpacityPercent(), elements.feedXPercent(), elements.feedYPercent(),
                screenWidth, screenHeight);
    }

    /** 自定义模块：按中心锚点换算屏幕矩形。 */
    public static Rect custom(ClientHudLayout.CustomElement element, int screenWidth, int screenHeight) {
        float scale = element.placement().fit(screenWidth, screenHeight);
        int width = Math.max(1, Math.min(screenWidth - 16, Math.round(element.width() * scale)));
        int height = Math.max(1, Math.min(screenHeight - 16, Math.round(element.height() * scale)));
        int centerX = clamp(Math.round(screenWidth * element.xPercent() / 100F
                + element.placement().offsetX() * scale), 8 + width / 2, screenWidth - 8 - (width + 1) / 2);
        int centerY = clamp(Math.round(screenHeight * element.yPercent() / 100F
                + element.placement().offsetY() * scale), 8 + height / 2, screenHeight - 8 - (height + 1) / 2);
        return new Rect(centerX - width / 2, centerY - height / 2, width, height, scale,
                element.width(), element.height());
    }

    /** 大厅场景横幅（正在匹配 / 房间中）：两行文字，中心锚点。 */
    public static Rect banner(Elements elements, int screenWidth, int screenHeight, int minTextWidth) {
        return anchored(Math.max(240, minTextWidth), BANNER_BASE_HEIGHT,
                elements.bannerScalePercent(), elements.bannerOpacityPercent(),
                elements.bannerXPercent(), elements.bannerYPercent(), screenWidth, screenHeight);
    }

    private static Rect anchored(int baseWidth, int baseHeight, int scalePercent, int opacityPercent,
                                 int xPercent, int yPercent, int screenWidth, int screenHeight) {
        float scale = fitScale(scalePercent / 100.0F, baseWidth, baseHeight,
                screenWidth - SCREEN_MARGIN * 2, Math.max(1, screenHeight - SCREEN_MARGIN * 2));
        int width = scaled(baseWidth, scale);
        int height = scaled(baseHeight, scale);
        int centerX = centered(screenWidth, xPercent, width,
                SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - SCREEN_MARGIN));
        int centerY = centered(screenHeight, yPercent, height,
                SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenHeight - SCREEN_MARGIN));
        return new Rect(centerX - width / 2, centerY - height / 2, width, height, scale,
                baseWidth, baseHeight);
    }

    private static float fitScale(float requested, int baseWidth, int baseHeight,
                                  int availableWidth, int availableHeight) {
        if (baseWidth <= 0 || baseHeight <= 0 || availableWidth <= 0 || availableHeight <= 0) {
            return 1.0F;
        }
        float fitted = Math.min(availableWidth / (float) baseWidth,
                availableHeight / (float) baseHeight);
        return Math.max(0.01F, Math.min(2.0F, Math.min(requested, fitted)));
    }

    private static int scaled(int baseSize, float scale) {
        return Math.max(8, Math.round(baseSize * scale));
    }

    private static int centered(int screenSize, int percent, int elementSize, int minimum, int maximum) {
        int half = elementSize / 2;
        return clamp(Math.round(screenSize * percent / 100.0F),
                Math.max(minimum + half, half), Math.min(maximum - half, screenSize - half));
    }

    private static int clampedTop(int screenSize, int percent, int elementSize, int minimum, int maximum) {
        return clamp(Math.round(screenSize * percent / 100.0F), minimum,
                Math.max(minimum, maximum - elementSize));
    }

    private static int clamp(int value, int minimum, int maximum) {
        return maximum < minimum ? minimum : Math.max(minimum, Math.min(maximum, value));
    }
}
