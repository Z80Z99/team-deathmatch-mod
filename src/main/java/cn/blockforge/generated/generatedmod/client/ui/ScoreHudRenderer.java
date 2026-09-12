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
        int teamCount = parseTeamCount(values.get("team_count"));
        if (teamCount >= 3) {
            drawMultiple(graphics, font, rect, elements, values);
            return;
        }
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
    private static void drawMultiple(GuiGraphics graphics, Font font, HudGeometry.Rect rect,
                                     ClientHudLayout.Elements elements, Map<String, String> values) {
        var teams = cn.blockforge.generated.generatedmod.match.Team.playing(
                parseTeamCount(values.get("team_count")));
        int w = rect.baseWidth(), h = rect.baseHeight(), opacity = elements.scoreOpacityPercent();
        graphics.pose().pushPose();
        graphics.pose().translate(rect.centerX(), rect.top(), 0);
        graphics.pose().scale(rect.scale(), rect.scale(), 1);
        graphics.fill(-w / 2, 0, w / 2, h, UiTheme.withAlpha(UiTheme.PANEL, opacity));
        int heading = UiTheme.withAlpha(elements.scoreColor(), opacity);
        int details = UiTheme.withAlpha(elements.scoreColor(), Math.max(20, opacity / 2));
        graphics.drawString(font, resolve(font, elements.scoreHeaderTemplate(), values, w - 82),
                -w / 2 + 6, 5, heading, false);
        graphics.drawString(font, resolve(font, elements.scoreTimerTemplate(), values, 64),
                w / 2 - 70, 5, heading, false);
        for (int i = 0; i < teams.size(); i++) {
            var team = teams.get(i);
            int left = -w / 2 + w * i / teams.size(), right = -w / 2 + w * (i + 1) / teams.size();
            String suffix = team.key().substring(5);
            int teamColor = UiTheme.withAlpha(team.hudColor(), opacity);
            graphics.fill(left + 3, 19, right - 3, 21, teamColor);
            graphics.drawCenteredString(font, UiTheme.fit(font, team.displayName() + " " + values.getOrDefault("score_" + suffix, "0"),
                    right - left - 8), (left + right) / 2, 26, teamColor);
            graphics.drawCenteredString(font, UiTheme.fit(font, "胜 " + values.getOrDefault("wins_" + suffix, "0"),
                    right - left - 8), (left + right) / 2, 39, details);
        }
        graphics.drawCenteredString(font, resolve(font, elements.scoreDetailsTemplate(), values, w - 16),
                0, h - 12, details);
        graphics.pose().popPose();
    }
    private static String resolve(Font font, String template, Map<String, String> values, int width) {
        return UiTheme.fit(font, HudStats.resolveTemplate(template, values), width);
    }

    private static int parseTeamCount(String value) {
        try {
            return Math.max(2, Math.min(4, Integer.parseInt(value)));
        } catch (RuntimeException error) {
            return 2;
        }
    }
}
