package cn.blockforge.generated.generatedmod.client.ui;

import cn.blockforge.generated.generatedmod.client.ClientHudLayout;
import cn.blockforge.generated.generatedmod.client.HudGeometry;
import cn.blockforge.generated.generatedmod.client.HudStats;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.Map;

/** Shared by live matches and the layout editor. Each score owns one third. */
public final class ScoreHudRenderer {
    private ScoreHudRenderer() { }

    public static void draw(GuiGraphics graphics, Font font, HudGeometry.Rect rect,
                            ClientHudLayout.Elements elements, Map<String, String> values, boolean pulse) {
        int w = rect.baseWidth();
        int h = rect.baseHeight();
        int opacity = elements.scoreOpacityPercent();
        int text = UiTheme.withAlpha(elements.scoreColor(), opacity);
        int a = UiTheme.withAlpha(UiTheme.TEAM_A, opacity);
        int b = UiTheme.withAlpha(UiTheme.TEAM_B, opacity);
        int cell = w / 3;
        graphics.pose().pushPose();
        graphics.pose().translate(rect.centerX(), rect.top(), 0);
        graphics.pose().scale(rect.scale(), rect.scale(), 1);
        graphics.fill(-w / 2, 0, w / 2, h, UiTheme.withAlpha(UiTheme.PANEL, opacity));
        graphics.fill(-w / 2, 18, -w / 2 + cell, h - 18, UiTheme.withAlpha(UiTheme.TEAM_A, opacity / 8));
        graphics.fill(w / 2 - cell, 18, w / 2, h - 18, UiTheme.withAlpha(UiTheme.TEAM_B, opacity / 8));
        graphics.fill(-w / 2, 18, -w / 2 + 2, h - 18, a);
        graphics.fill(w / 2 - 2, 18, w / 2, h - 18, b);
        graphics.drawCenteredString(font, resolve(font, elements.scoreHeaderTemplate(), values, w - 16), 0, 5, UiTheme.withAlpha(UiTheme.MUTED, opacity));
        graphics.drawCenteredString(font, resolve(font, elements.scoreTeamATemplate(), values, cell - 12), -w / 2 + cell / 2, 29, a);
        graphics.drawCenteredString(font, resolve(font, elements.scoreTeamBTemplate(), values, cell - 12), w / 2 - cell / 2, 29, b);
        graphics.drawCenteredString(font, resolve(font, elements.scoreTimerTemplate(), values, cell - 12), 0, 29, text);
        graphics.drawCenteredString(font, resolve(font, elements.scoreDetailsTemplate(), values, w - 16), 0, h - 12, UiTheme.withAlpha(UiTheme.MUTED, opacity));
        if (pulse) graphics.fill(-w / 2, h - 2, w / 2, h, UiTheme.withAlpha(UiTheme.ACCENT, opacity));
        graphics.pose().popPose();
    }
    private static String resolve(Font font, String template, Map<String, String> values, int width) {
        return UiTheme.fit(font, HudStats.resolveTemplate(template, values), width);
    }
}
