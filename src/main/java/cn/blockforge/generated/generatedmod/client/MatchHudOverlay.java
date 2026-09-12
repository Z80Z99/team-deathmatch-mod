package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.ClientHudLayout.Elements;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.match.MatchState;
import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.match.ClassicBombState;
import cn.blockforge.generated.generatedmod.client.screen.HudLayoutScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.LinkedHashMap;
import java.util.List;
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
        if (ClientMatchData.state == MatchState.TERRAIN_RESTORING) {
            graphics.fill(0, 0, width, height, 0xFF030303);
            return;
        }
        Elements elements = ClientHudLayout.elements(HudContext.match(ClientMatchData.mode));
        ClientHudLayout.Global global = ClientHudLayout.global();
        if (!global.backgroundFile().isBlank()) {
            HudBackground.drawStretch(graphics, global.backgroundFile(),
                    global.backgroundOpacityPercent(), width, height);
        }
        renderBoundaryFilters(graphics, width, height);
        HudContext context = HudContext.match(ClientMatchData.mode);
        HudCustomRenderer.render(graphics, forgeGui.getMinecraft().font,
                ClientHudLayout.customElements(HudContext.GLOBAL), width, height, false, false,
                "", HudContext.GLOBAL);
        if (elements.builtInEnabled(HudContext.BuiltIn.SCORE) && elements.scoreVisible() && !renderOverride(graphics, forgeGui.getMinecraft().font, context,
                HudContext.BuiltIn.SCORE, elements, width, height, partialTick)) {
            renderScore(forgeGui, graphics, width, height, elements, context);
        }
        if (elements.builtInEnabled(HudContext.BuiltIn.NOTICE) && elements.textVisible()
                && !renderOverride(graphics, forgeGui.getMinecraft().font, context,
                HudContext.BuiltIn.NOTICE, elements, width, height, partialTick)) {
            renderNotice(forgeGui, graphics, width, height, elements);
        }
        if (elements.builtInEnabled(HudContext.BuiltIn.FEED) && elements.feedVisible() && ClientMatchData.killFeedActive()
                && !renderOverride(graphics, forgeGui.getMinecraft().font, context,
                HudContext.BuiltIn.FEED, elements, width, height, partialTick)) {
            renderKillFeed(forgeGui, graphics, width, height, elements, context);
        }
        if (!RespawnOverlay.active() && elements.builtInEnabled(HudContext.BuiltIn.TEXT) && elements.textVisible() && !renderOverride(graphics, forgeGui.getMinecraft().font, context,
                HudContext.BuiltIn.TEXT, elements, width, height, partialTick)) {
            renderTeamHint(forgeGui, graphics, width, height, elements, context);
        }
        renderCustom(graphics, forgeGui.getMinecraft().font, width, height,
                HudContext.match(ClientMatchData.mode));
        renderBoundaryWarning(graphics, forgeGui.getMinecraft().font, width, height, context);
    }

    private static void renderBoundaryFilters(GuiGraphics graphics, int width, int height) {
        if (ClientMatchData.boundaryTicks <= 0) return;
        HudContext context = HudContext.match(ClientMatchData.mode);
        List<ClientHudLayout.CustomElement> elements = ClientHudLayout.customElements(context);
        ClientHudLayout.CustomElement warning = elements.stream()
                .filter(element -> element.type().equals("boundary"))
                .findFirst().orElse(null);
        float opacity = warning == null ? 1.0F
                : Math.max(0, Math.min(100, warning.opacityPercent())) / 100.0F;
        int redAlpha = Math.round(255 * BoundaryEffects.redOpacity(ClientMatchData.boundaryTicks) * opacity);
        int blackAlpha = Math.round(255 * BoundaryEffects.blackOpacity(ClientMatchData.boundaryTicks) * opacity);
        int neutralAlpha = Math.round(255 * 0.10F * BoundaryEffects.finalPhase(ClientMatchData.boundaryTicks) * opacity);
        graphics.fill(0, 0, width, height, (redAlpha << 24) | 0xC41624);
        if (blackAlpha > 0) graphics.fill(0, 0, width, height, blackAlpha << 24);
        if (neutralAlpha > 0) graphics.fill(0, 0, width, height, (neutralAlpha << 24) | 0xAEB3B5);
    }

    private static void renderBoundaryWarning(GuiGraphics graphics, Font font, int width, int height,
                                              HudContext context) {
        if (!ClientMatchData.boundaryOutside || ClientMatchData.boundaryTicks <= 0) return;
        List<ClientHudLayout.CustomElement> elements = ClientHudLayout.customElements(context);
        List<ClientHudLayout.CustomElement> warnings = elements.stream()
                .filter(element -> element.type().equals("boundary") && element.visible()
                        && HudConditions.visible(element, elements, false, context))
                .toList();
        if (warnings.isEmpty()) return;
        for (ClientHudLayout.CustomElement warning : warnings) {
            renderBoundaryCard(graphics, font, warning, context, width, height);
        }
    }

    private static void renderBoundaryCard(GuiGraphics graphics, Font font,
                                           ClientHudLayout.CustomElement warning,
                                           HudContext context, int width, int height) {
        double amount = HudAnimation.frame(HudAnimation.key(warning.id()), true,
                warning.placement());
        if (amount <= 0.0D) return;
        HudGeometry.Rect rect = HudGeometry.custom(warning, width, height);
        float alpha = (float) amount;
        int opacity = Math.max(0, Math.min(100, warning.opacityPercent()));
        if (warning.shadow()) graphics.fill(rect.left() + 2, rect.top() + 2,
                rect.right() + 2, rect.bottom() + 2,
                UiTheme.withAlpha(warning.placement().shadowColor() == 0 ? UiTheme.SHADOW
                        : warning.placement().shadowColor(), (int) (opacity * alpha)));
        if (warning.background()) graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(),
                UiTheme.withAlpha(warning.placement().backgroundColor() == 0 ? UiTheme.PANEL_RAISED
                        : warning.placement().backgroundColor(), (int) (opacity * alpha)));
        if (warning.border()) graphics.renderOutline(rect.left(), rect.top(), rect.width(), rect.height(),
                UiTheme.withAlpha(warning.placement().borderColor() == 0 ? warning.color()
                        : warning.placement().borderColor(), (int) (opacity * alpha)));
        String label = HudCustomRenderer.resolveText(warning, context, false);
        if (label.isBlank()) label = "返回作战区域";
        float scale = Math.max(0.5F, Math.min(3.0F, warning.scalePercent() / 100.0F));
        drawAlignedText(graphics, font, label, rect.left() + 5, rect.right() - 5,
                rect.top() + 8, warning.placement().alignment(), scale, 0xFFFFD6D6);
        drawAlignedText(graphics, font,
                Integer.toString((ClientMatchData.boundaryTicks + 19) / 20),
                rect.left() + 5, rect.right() - 5, rect.top() + 27,
                warning.placement().alignment(), scale * 1.25F, 0xFFFFFFFF);
        int barY = Math.max(rect.top() + 5, rect.bottom() - 6);
        float fallback = Math.max(0, Math.min(1,
                ClientMatchData.boundaryTicks / (float) cn.blockforge.generated.generatedmod.match.BoundaryCountdown.DURATION));
        double ratio = HudCustomRenderer.resolveRatio(warning, context, false, fallback);
        int filled = (int) Math.round(rect.width() * ratio);
        graphics.fill(rect.left(), barY, rect.right(), Math.min(rect.bottom(), barY + 3),
                UiTheme.withAlpha(UiTheme.BORDER, (int) (opacity * alpha)));
        if ("drain".equals(warning.placement().progressDirection())) {
            graphics.fill(rect.right() - filled, barY, rect.right(),
                    Math.min(rect.bottom(), barY + 3),
                    UiTheme.withAlpha(warning.color(), (int) (opacity * alpha)));
        } else {
            graphics.fill(rect.left(), barY, rect.left() + filled,
                    Math.min(rect.bottom(), barY + 3),
                    UiTheme.withAlpha(warning.color(), (int) (opacity * alpha)));
        }
        HudCustomRenderer.glow(graphics, warning, rect, (int) (255 * alpha));
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
                width, height, false, false, "", context);
    }

    /** 击杀播报：位置 / 大小 / 透明度独立可调，出现约 6 秒并淡出。 */
    private static void renderKillFeed(ForgeGui forgeGui, GuiGraphics graphics, int width, int height,
                                       Elements elements, HudContext context) {
        Font font = forgeGui.getMinecraft().font;
        String text = HudParameters.render(elements.feedTemplate(), context, false);
        int fade = ClientMatchData.killFeedTicksLeft() < 20
                ? 255 * ClientMatchData.killFeedTicksLeft() / 20 : 255;
        int panelAlpha = Math.min(elements.feedOpacityPercent(), 100) * fade / 255;
        int baseWidth = font.width(text) + 20;
        HudGeometry.Rect panel = HudGeometry.feed(elements, width, height, baseWidth);
        drawPanel(graphics, font, panel, HudGeometry.FEED_BASE_HEIGHT, text, "", elements.feedColor(), panelAlpha);
    }

    private static void renderScore(ForgeGui forgeGui, GuiGraphics graphics, int width, int height,
                                    Elements elements, HudContext context) {
        cn.blockforge.generated.generatedmod.client.ui.ScoreHudRenderer.draw(graphics, forgeGui.getMinecraft().font,
                HudGeometry.score(elements, width, height), elements, templateValues(context),
                ClientMatchData.pulseActive());
    }

    private static void renderNotice(ForgeGui forgeGui, GuiGraphics graphics, int width, int height,
                                     Elements elements) {
        String title = MatchHudNotice.title();
        String detail = MatchHudNotice.detail();
        String timer = MatchHudNotice.timer();
        String first = timer == null || timer.isBlank() ? title : title + "  " + timer;
        HudGeometry.Rect panel = HudGeometry.banner(elements, width, height, 300);
        drawPanel(graphics, forgeGui.getMinecraft().font, panel, HudGeometry.BANNER_BASE_HEIGHT,
                first, detail, elements.bannerColor(), elements.bannerOpacityPercent());
    }

    private static void renderTeamHint(ForgeGui forgeGui, GuiGraphics graphics, int width, int height,
                                       Elements elements, HudContext context) {
        Font font = forgeGui.getMinecraft().font;
        String hint = HudParameters.render(elements.textTemplate(), context, false);
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
        String bombStatus = bombStatusText();
        if (bombStatus != null) return bombStatus;
        if (ClientMatchData.state == MatchState.MATCH_END) {
            return ClientMatchData.winner == null ? "比赛平局" : ClientMatchData.winner.displayName() + " 获胜";
        }
        if (ClientMatchData.state == MatchState.MAP_RESETTING) {
            return "地图恢复中，请稍候";
        }
        if (ClientMatchData.state == MatchState.TERRAIN_RESTORING) {
            return "地形恢复中，请稍候";
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

    public static String bombStatusText() {
        if (ClientMatchData.mode != cn.blockforge.generated.generatedmod.match.GameMode.SEARCH_DESTROY
                || !ClientBombData.active) return null;
        return switch (ClientBombData.phase) {
            case PLANTED -> "C4 " + ClientBombData.bombSiteName + "  " + String.format(java.util.Locale.ROOT,
                    "%.1f", ClientBombData.detonationRemainingTicks / 20.0D);
            case PLANTING -> "安装中  " + String.format(java.util.Locale.ROOT, "%.1f",
                    ClientBombData.actionRemainingTicks / 20.0D);
            case DEFUSING -> "拆除中  " + String.format(java.util.Locale.ROOT, "%.1f",
                    ClientBombData.actionRemainingTicks / 20.0D);
            case DROPPED -> "C4 已掉落";
            case CARRIED -> ClientBombData.carrierName.isBlank() ? "C4 已发放"
                    : "C4 携带者：" + ClientBombData.carrierName;
            default -> null;
        };
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

    private static void drawAlignedText(GuiGraphics graphics, Font font, String text,
                                        int left, int right, int y, String alignment,
                                        float scale, int color) {
        if (text == null || text.isBlank()) return;
        String fitted = font.plainSubstrByWidth(text,
                Math.max(1, Math.round((right - left) / Math.max(0.5F, scale))));
        float width = font.width(fitted) * scale;
        float x = switch (alignment == null ? "center" : alignment) {
            case "left" -> left;
            case "right" -> right - width;
            default -> (left + right - width) / 2.0F;
        };
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, fitted, 0, 0, color, false);
        graphics.pose().popPose();
    }

    /** 当前比赛模板可用的全部占位符，供内置组件和第三方 HUD 共用。 */
    public static Map<String, String> templateValues() {
        return templateValues(HudContext.match(ClientMatchData.mode));
    }

    public static Map<String, String> templateValues(HudContext context) {
        Map<String, String> values = new LinkedHashMap<>(
                HudParameters.values(false, context, true));
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
        String bombStatus = bombStatusText();
        values.put("bomb_status", bombStatus == null ? "" : bombStatus);
        values.put("killer", ClientMatchData.killFeedKiller());
        values.put("victim", ClientMatchData.killFeedVictim());
        values.put("feed", ClientMatchData.killFeedText());
        return values;
    }
}
