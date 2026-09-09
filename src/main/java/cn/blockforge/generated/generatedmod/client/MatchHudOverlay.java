package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.ClientHudLayout.Elements;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.match.MatchState;
import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.client.screen.HudLayoutScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 比赛 HUD：只显示服务器同步的数据；当前模式的配置场景（团队死斗 / 爆破 / 歼灭
 * 各一份）由 {@link HudContext} 决定，元素位置与“HUD 配置窗”共用
 * {@link HudGeometry}，做到编辑器所见即所得。
 */
public final class MatchHudOverlay {
    private static final int SCREEN_MARGIN = HudGeometry.SCREEN_MARGIN;

    private MatchHudOverlay() {
    }

    public static void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick, int width, int height) {
        // 模组配置界面会绘制自己的内容；实时 HUD 必须让出整个屏幕，避免盖住配置面板。
        if (Minecraft.getInstance().screen instanceof UiScreen
                || Minecraft.getInstance().screen instanceof HudLayoutScreen
                || !ClientMatchData.inMatch() || width <= 0 || height <= 0) {
            return;
        }
        Elements elements = ClientHudLayout.elements(HudContext.match(ClientMatchData.mode));
        ClientHudLayout.Global global = ClientHudLayout.global();
        if (!global.backgroundFile().isBlank()) {
            HudBackground.drawStretch(graphics, global.backgroundFile(),
                    global.backgroundOpacityPercent(), width, height);
        }
        HudContext context = HudContext.match(ClientMatchData.mode);
        if (elements.builtInEnabled(HudContext.BuiltIn.SCORE) && elements.scoreVisible() && !renderOverride(graphics, forgeGui.getMinecraft().font, context,
                HudContext.BuiltIn.SCORE, elements, width, height, partialTick)) {
            renderScore(forgeGui, graphics, width, height, elements);
        }
        if (elements.builtInEnabled(HudContext.BuiltIn.FEED) && elements.feedVisible() && ClientMatchData.killFeedActive()
                && !renderOverride(graphics, forgeGui.getMinecraft().font, context,
                HudContext.BuiltIn.FEED, elements, width, height, partialTick)) {
            renderKillFeed(forgeGui, graphics, width, height, elements);
        }
        if (!RespawnOverlay.active() && elements.builtInEnabled(HudContext.BuiltIn.TEXT) && elements.textVisible() && !renderOverride(graphics, forgeGui.getMinecraft().font, context,
                HudContext.BuiltIn.TEXT, elements, width, height, partialTick)) {
            renderTeamHint(forgeGui, graphics, width, height, elements);
        }
        renderCustom(graphics, forgeGui.getMinecraft().font, width, height,
                HudContext.match(ClientMatchData.mode));
        if (ClientMatchData.boundaryTicks > 0) {
            int alpha = 35 + (200 - Math.min(200, ClientMatchData.boundaryTicks)) * 105 / 200;
            graphics.fill(0, 0, width, height, (alpha << 24) | 0x650008);
            Font font = forgeGui.getMinecraft().font;
            graphics.drawCenteredString(font, "返回作战区域", width / 2, height / 3, 0xFFFFD6D6);
            graphics.drawCenteredString(font, Integer.toString((ClientMatchData.boundaryTicks + 19) / 20),
                    width / 2, height / 3 + 20, 0xFFFFFFFF);
        }
    }

    private static boolean renderOverride(GuiGraphics graphics, Font font, HudContext context,
                                          HudContext.BuiltIn component, Elements elements,
                                          int width, int height, float partialTick) {
        HudApi.BuiltInRenderer renderer = HudApi.builtInRenderer(context, component);
        if (renderer == null) {
            return false;
        }
        try {
            return renderer.render(graphics, font, context, component, elements, width, height, partialTick);
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static void renderCustom(GuiGraphics graphics, Font font, int width, int height,
                                     HudContext context) {
        HudCustomRenderer.render(graphics, font, ClientHudLayout.customElements(context),
                width, height, false, false);
    }

    /** 击杀播报：位置 / 大小 / 透明度独立可调，出现约 6 秒并淡出。 */
    private static void renderKillFeed(ForgeGui forgeGui, GuiGraphics graphics, int width, int height,
                                       Elements elements) {
        Font font = forgeGui.getMinecraft().font;
        String text = HudStats.resolveTemplate(elements.feedTemplate(), Map.of(
                "killer", ClientMatchData.killFeedKiller(), "victim", ClientMatchData.killFeedVictim(),
                "feed", ClientMatchData.killFeedText()));
        int fade = ClientMatchData.killFeedTicksLeft() < 20
                ? 255 * ClientMatchData.killFeedTicksLeft() / 20 : 255;
        int panelAlpha = Math.min(elements.feedOpacityPercent(), 100) * fade / 255;
        int baseWidth = font.width(text) + 20;
        HudGeometry.Rect panel = HudGeometry.feed(elements, width, height, baseWidth);
        drawPanel(graphics, font, panel, HudGeometry.FEED_BASE_HEIGHT, text, "", elements.feedColor(), panelAlpha);
    }

    private static void renderScore(ForgeGui forgeGui, GuiGraphics graphics, int width, int height,
                                    Elements elements) {
        cn.blockforge.generated.generatedmod.client.ui.ScoreHudRenderer.draw(graphics, forgeGui.getMinecraft().font,
                HudGeometry.score(elements, width, height), elements, templateValues(), ClientMatchData.pulseActive());
    }

    private static void renderTeamHint(ForgeGui forgeGui, GuiGraphics graphics, int width, int height,
                                       Elements elements) {
        Font font = forgeGui.getMinecraft().font;
        String hint = HudStats.resolveTemplate(elements.textTemplate(), Map.of(
                "team", ClientMatchData.myTeam.displayName(), "sizes", ClientMatchData.teamSizesText(),
                "hint", hintText(), "mode", ClientMatchData.modeText(), "phase", ClientMatchData.phaseText()));
        HudGeometry.Rect panel = HudGeometry.text(elements, width, height, elements.textWidth());
        drawPanel(graphics, font, panel, HudGeometry.TEXT_BASE_HEIGHT, hint, "", elements.textColor(), elements.textOpacityPercent());
    }

    /** 通用面板画法：供比赛文字条与大厅场景横幅共用。 */
    public static void drawPanel(GuiGraphics graphics, Font font, HudGeometry.Rect panel, int baseHeight,
                          String lineOne, String lineTwo, int accent, int opacity) {
        graphics.pose().pushPose();
        graphics.pose().translate(panel.centerX(), panel.centerY(), 0.0F);
        graphics.pose().scale(panel.scale(), panel.scale(), 1.0F);
        int baseWidth = panel.baseWidth();
        graphics.fill(-baseWidth / 2, -baseHeight / 2, baseWidth / 2, baseHeight / 2,
                UiTheme.withAlpha(UiTheme.PANEL_RAISED, opacity));
        graphics.fill(-baseWidth / 2 + 1, -baseHeight / 2 + 1, -baseWidth / 2 + 4,
                baseHeight / 2 - 1, UiTheme.withAlpha(accent, opacity));
        if (lineTwo == null || lineTwo.isBlank()) {
            graphics.drawCenteredString(font, fit(font, lineOne, baseWidth - 18), 0, -4,
                    UiTheme.withAlpha(accent, opacity));
        } else {
            graphics.drawCenteredString(font, fit(font, lineOne, baseWidth - 18), 0, -11,
                    UiTheme.withAlpha(UiTheme.TEXT, opacity));
            graphics.drawCenteredString(font, fit(font, lineTwo, baseWidth - 18), 0, 3,
                    UiTheme.withAlpha(UiTheme.MUTED, opacity));
        }
        graphics.pose().popPose();
    }

    /** 底部状态文字的内容与配色（编辑器预览复用）。 */
    public static String hintText() {
        if (ClientMatchData.state == MatchState.MATCH_END) {
            return ClientMatchData.winner == null ? "比赛平局" : ClientMatchData.winner.displayName() + " 获胜";
        }
        if (ClientMatchData.state == MatchState.MAP_RESETTING) {
            return "地图恢复中，请稍候";
        }
        if (ClientMatchData.respawnRemainingTicks > 0) {
            return "状态恢复倒计时  " + ClientMatchData.respawnTimerText();
        }
        if (ClientMatchData.pending) {
            return "已加入，等待下一回合入场";
        }
        if (ClientMatchData.myTeam == Team.SPECTATOR) {
            return "观战  ·  " + ClientMatchData.teamSizesText();
        }
        if (ClientMatchData.state == MatchState.WARMUP) {
            return ClientMatchData.myTeam.displayName() + "  ·  回合开始倒计时  "
                    + ClientMatchData.phaseTimerText();
        }
        return "队伍：" + ClientMatchData.myTeam.displayName() + "  ·  " + ClientMatchData.teamSizesText();
    }

    private static int hintColor() {
        if (ClientMatchData.state == MatchState.MATCH_END) {
            return ClientMatchData.winner == null ? UiTheme.MUTED : teamColor(ClientMatchData.winner);
        }
        if (ClientMatchData.state == MatchState.MAP_RESETTING) {
            return UiTheme.MUTED;
        }
        if (ClientMatchData.respawnRemainingTicks > 0 || ClientMatchData.state == MatchState.WARMUP) {
            return UiTheme.WARNING;
        }
        if (ClientMatchData.pending) {
            return UiTheme.MUTED;
        }
        if (ClientMatchData.myTeam == Team.SPECTATOR) {
            return UiTheme.SPECTATOR;
        }
        return teamColor(ClientMatchData.myTeam);
    }

    private static int teamColor(Team team) {
        return switch (team) {
            case TEAM_A -> UiTheme.TEAM_A;
            case TEAM_B -> UiTheme.TEAM_B;
            case TEAM_C, TEAM_D -> ClientMatchData.myTeam.hudColor();
            case SPECTATOR -> UiTheme.SPECTATOR;
        };
    }

    private static String fit(Font font, String value, int maximumWidth) {
        return UiTheme.fit(font, value, maximumWidth);
    }

    /** 当前比赛模板可用的全部占位符，供内置组件和第三方 HUD 共用。 */
    public static Map<String, String> templateValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("mode", ClientMatchData.modeText());
        values.put("phase", ClientMatchData.phaseText());
        values.put("score_a", Integer.toString(ClientMatchData.teamAScore));
        values.put("score_b", Integer.toString(ClientMatchData.teamBScore));
        values.put("team_count", Integer.toString(Math.max(2, ClientMatchData.teamStats.size())));
        for (var stats : ClientMatchData.teamStats) {
            String suffix = stats.team().key().substring(5);
            values.put("score_" + suffix, Integer.toString(stats.score()));
            values.put("wins_" + suffix, Integer.toString(stats.wins()));
        }
        values.put("time", ClientMatchData.phaseTimerText());
        values.put("round", Integer.toString(ClientMatchData.roundNumber));
        values.put("target", Integer.toString(HudParameters.target()));
        values.put("team", ClientMatchData.myTeam.displayName());
        values.put("sizes", ClientMatchData.teamSizesText());
        values.put("hint", hintText());
        values.put("killer", ClientMatchData.killFeedKiller());
        values.put("victim", ClientMatchData.killFeedVictim());
        values.put("feed", ClientMatchData.killFeedText());
        return values;
    }
}
