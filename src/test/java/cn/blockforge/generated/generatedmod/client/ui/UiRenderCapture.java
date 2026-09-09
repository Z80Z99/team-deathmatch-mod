package cn.blockforge.generated.generatedmod.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.mockito.Answers;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.mockito.Mockito.mock;

/** Diagnostic images of actual GUI draw calls, not an OpenGL/game screenshot. */
final class UiRenderCapture implements AutoCloseable {
    private static final Font LABEL_FONT = new Font("Microsoft YaHei", Font.PLAIN, 9);
    private BufferedImage image;
    private Graphics2D canvas;
    private final PoseStack pose = new PoseStack();
    final GuiGraphics graphics;
    float opaquePanelDepth = Float.NEGATIVE_INFINITY;

    UiRenderCapture(int width, int height) {
        image = new BufferedImage(width * 3, height * 3, BufferedImage.TYPE_INT_ARGB);
        canvas = image.createGraphics();
        canvas.setColor(new Color(0x101113)); canvas.fillRect(0, 0, image.getWidth(), image.getHeight());
        canvas.setFont(LABEL_FONT);
        graphics = mock(GuiGraphics.class, call -> {
            String method = call.getMethod().getName(); Object[] args = call.getArguments();
            var matrix = pose.last().pose();
            canvas.setTransform(new AffineTransform(matrix.m00() * 3, matrix.m01() * 3,
                    matrix.m10() * 3, matrix.m11() * 3, matrix.m30() * 3, matrix.m31() * 3));
            if (method.equals("pose")) return pose;
            if (method.equals("fillGradient") && args.length == 6) {
                int x0 = (int) args[0], y0 = (int) args[1], x1 = (int) args[2], y1 = (int) args[3];
                canvas.setPaint(new java.awt.GradientPaint(x0, y0, new Color((int) args[4], true),
                        x0, y1, new Color((int) args[5], true)));
                canvas.fillRect(x0, y0, x1 - x0, y1 - y0);
                return null;
            }
            if (method.equals("fill") && args.length == 5 && args[0] instanceof Integer) {
                if ((int) args[4] == (UiTheme.PANEL | 0xFF000000)) {
                    opaquePanelDepth = Math.max(opaquePanelDepth, matrix.m32());
                }
                canvas.setColor(new Color((int) args[4], true));
                canvas.fillRect((int) args[0], (int) args[1], (int) args[2] - (int) args[0], (int) args[3] - (int) args[1]); return null;
            }
            if (method.equals("renderOutline")) {
                canvas.setColor(new Color((int) args[4], true)); canvas.drawRect((int) args[0], (int) args[1], (int) args[2] - 1, (int) args[3] - 1); return null;
            }
            if (method.equals("drawString") || method.equals("drawCenteredString")) {
                String value = text(args[1]);
                int x = ((Number) args[2]).intValue(); int y = ((Number) args[3]).intValue();
                if (method.equals("drawCenteredString")) x -= measure(value) / 2;
                canvas.setColor(new Color((int) args[4], true)); canvas.drawString(value, x, y + 8);
                return call.getMethod().getReturnType() == int.class ? measure(value) : null;
            }
            if (method.equals("enableScissor")) { canvas.setClip((int) args[0], (int) args[1], (int) args[2] - (int) args[0], (int) args[3] - (int) args[1]); return null; }
            if (method.equals("disableScissor")) { canvas.setClip(null); return null; }
            return Answers.RETURNS_DEFAULTS.answer(call);
        });
    }
    static int measure(String value) {
        return value == null ? 0 : value.codePoints().map(c -> c < 128 ? 5 : 9).sum();
    }
    static String shorten(String value, int width, boolean backwards) {
        int begin = 0, end = value.length();
        while (begin < end && measure(value.substring(begin, end)) > width) {
            if (backwards) begin = value.offsetByCodePoints(begin, 1); else end = value.offsetByCodePoints(end, -1);
        }
        return value.substring(begin, end);
    }
    private static String text(Object value) {
        if (value instanceof String text) return text;
        if (value instanceof Component text) return text.getString();
        if (value instanceof FormattedCharSequence sequence) {
            StringBuilder text = new StringBuilder();
            sequence.accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; }); return text.toString();
        }
        return "";
    }
    void save(String name) throws Exception {
        Path path = Path.of("build", "ui-previews", name + ".png"); Files.createDirectories(path.getParent()); ImageIO.write(image, "png", path.toFile());
    }
    @Override public void close() {
        canvas.dispose(); image.flush(); canvas = null; image = null;
        org.mockito.Mockito.clearInvocations(graphics);
    }
}
