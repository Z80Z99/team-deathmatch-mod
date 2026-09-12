package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.ClientHudLayout.*;
import cn.blockforge.generated.generatedmod.client.HudContext.BuiltIn;
import net.minecraft.client.Minecraft;

/** Example assemblies are flattened at creation time, not rendered as indivisible panels. */
public final class HudAssemblies {
    private HudAssemblies() { }

    public static void splitAll(Draft draft, HudContext context) {
        if (context == HudContext.GLOBAL) return;
        for (BuiltIn component : BuiltIn.values()) {
            split(draft, context, component);
        }
    }

    public static void split(Draft draft, HudContext context, BuiltIn component) {
        Elements v = draft.elements(context);
        if (!v.builtInEnabled(component)) return;
        draft.of(context).setBuiltInEnabled(component, false);
        if (component == BuiltIn.NOTICE && !context.isMatch()) return;
        if (context.isMatch() == (component == BuiltIn.BANNER)) return;
        HudGeometry.Rect r = switch (component) {
            case SCORE -> HudGeometry.score(v, 640, 360);
            case NOTICE -> HudGeometry.banner(v, 640, 360, 300);
            case TEXT -> HudGeometry.text(v, 640, 360, 0);
            case FEED -> HudGeometry.feed(v, 640, 360, 240);
            case BANNER -> HudGeometry.banner(v, 640, 360, 340);
        };
        int xp = switch (component) {
            case SCORE -> v.scoreXPercent(); case NOTICE -> v.bannerXPercent(); case TEXT -> v.textXPercent();
            case FEED -> v.feedXPercent(); case BANNER -> v.bannerXPercent();
        };
        int yp = switch (component) {
            case SCORE -> v.scoreYPercent(); case NOTICE -> v.bannerYPercent(); case TEXT -> v.textYPercent();
            case FEED -> v.feedYPercent(); case BANNER -> v.bannerYPercent();
        };
        Builder b = new Builder(draft, context, "示例·" + component.displayName(), xp, yp);
        b.visible = switch (component) {
            case SCORE -> v.scoreVisible(); case NOTICE -> v.textVisible(); case TEXT -> v.textVisible();
            case FEED -> v.feedVisible(); case BANNER -> v.bannerVisible();
        };
        b.opacity = switch (component) {
            case SCORE -> v.scoreOpacityPercent(); case NOTICE -> v.textOpacityPercent(); case TEXT -> v.textOpacityPercent();
            case FEED -> v.feedOpacityPercent(); case BANNER -> v.bannerOpacityPercent();
        };
        b.condition = component == BuiltIn.FEED ? "feed" : "";
        int x = r.centerX() - Math.round(640 * xp / 100F);
        int y = r.centerY() - Math.round(360 * yp / 100F);
        int w = r.width(), h = r.height();
        b.block("底板", x, y, w, h, 0xFF17191C);
        int color = switch (component) {
            case SCORE -> v.scoreColor(); case NOTICE -> v.textColor(); case TEXT -> v.textColor();
            case FEED -> v.feedColor(); case BANNER -> v.bannerColor();
        };
        b.block("强调线", x, y - h / 2 + 1, w, 2, color);
        if (component == BuiltIn.SCORE) {
            b.line("标题", v.scoreHeaderTemplate(), x, y - 26, w - 16, color);
            b.block("A队底板", x - w / 3, y, w / 3, 30, 0xFF44262B);
            b.block("B队底板", x + w / 3, y, w / 3, 30, 0xFF253548);
            b.line("A队", v.scoreTeamATemplate(), x - w / 3, y, w / 3 - 8, 0xFFFF6067);
            b.line("B队", v.scoreTeamBTemplate(), x + w / 3, y, w / 3 - 8, 0xFF66AEFF);
            b.line("计时", v.scoreTimerTemplate(), x, y, w / 3 - 8, color);
            b.line("详情", v.scoreDetailsTemplate(), x, y + 26, w - 16, 0xFFDADDE0);
            for (int i = 2; i < 4; i++) {
                String key = i == 2 ? "c" : "d";
                b.condition = "team_" + key;
                b.line(key.toUpperCase() + "队", key.toUpperCase() + "队 {score_" + key + "}",
                        x + (i == 2 ? -70 : 70), y + 50, 120, i == 2 ? 0xFF7BDC98 : 0xFFF4CE70);
            }
        } else if (component == BuiltIn.NOTICE) {
            b.condition = "notice";
            b.text("标题", "{notice_title}", x - Math.max(36, w / 5), y - 8,
                    Math.max(90, w / 2), color, 110);
            b.text("倒计时", "{notice_timer}", x + Math.max(34, w / 4), y - 8,
                    Math.max(72, w / 4), color, 105);
            b.line("详情", "{notice_detail}", x, y + 10, w - 16, 0xFFE8ECEF);
        } else if (component == BuiltIn.BANNER) {
            boolean matchingExample = context == HudContext.MATCHING
                    && v.bannerLineOneTemplate().equals("{matching_line1}")
                    && v.bannerLineTwoTemplate().equals("{matching_line2}");
            if (matchingExample) b.condition = "queued";
            b.line("第一行", v.bannerLineOneTemplate(), x, y - 7, w - 16, color);
            b.line("第二行", v.bannerLineTwoTemplate(), x, y + 7, w - 16, 0xFFDADDE0);
            if (matchingExample) {
                b.condition = "forming";
                b.line("开赛倒计时", "{matching_state} {ready} 秒后开赛", x, y - 7, w - 16, color);
                b.line("入场提示", "准备进入比赛", x, y + 7, w - 16, 0xFFDADDE0);
            }
        } else {
            b.line("文字", component == BuiltIn.TEXT ? v.textTemplate() : v.feedTemplate(),
                    x, y, w - 12, color);
        }
    }

    /** Adds one event-specific, independently editable HUD element set. */
    public static void addEventComponent(Draft draft, HudContext context,
                                         cn.blockforge.generated.generatedmod.match.MatchHudEventType type) {
        if (draft == null || context == null || type == null || !context.isMatch()) return;
        long existing = draft.customElements(context).stream()
                .filter(element -> element.placement().example().equals("事件·" + type.displayName()))
                .count();
        int anchorY = 20 + (int) (existing % 4) * 9;
        String prefix = "event:" + type.id();
        Builder b = new Builder(draft, context, "事件·" + type.displayName(), 50, anchorY);
        b.condition = "event:" + type.id();
        b.opacity = 88;
        b.block("事件底板", 0, 0, 300, 46, 0xFF11161B);
        b.block("事件强调线", 0, -23, 300, 2, 0xFF78D6A5);
        b.text("事件标题", "{" + prefix + ":title}", -95, -8, 140, 0xFF78D6A5, 110);
        b.text("事件计时", "{" + prefix + ":timer}", 118, -8, 68, 0xFFE8ECEF, 100);
        b.line("事件说明", "{" + prefix + ":detail}", 0, 9, 280, 0xFFE8ECEF);
        b.progress("事件进度", prefix + ":progress", 0, 17, 280, 0xFFFF7078);
    }

    public static final class Builder {
        final Draft draft;
        final HudContext context;
        final String example;
        final int anchorX, anchorY;
        public String condition = "";
        public boolean visible = true;
        public int opacity = 90;

        public Builder(Draft draft, HudContext context, String example, int anchorX, int anchorY) {
            this.draft = draft; this.context = context; this.example = example;
            this.anchorX = anchorX; this.anchorY = anchorY;
        }

        public void block(String name, int x, int y, int width, int height, int color) {
            atom(name, "block", "", x, y, width, height, color, "", 100);
        }

        public void text(String name, String template, int x, int y, int width, int color, int scale) {
            atom(name, "text", template, x, y, width, Math.max(12, scale * 14 / 100), color, "", scale);
        }

        public void progress(String name, String source, int x, int y, int width, int color) {
            atom(name, "progress", "", x, y, width, 3, color, source, 100);
        }

        public void line(String name, String template, int x, int y, int width, int color) {
            template = template.replace("{matching_line1}", "{matching_state} {queue}/{queue_need} 人成局")
                    .replace("{matching_line2}", "已等待 {wait} · 序位 {position}")
                    .replace("{room_line1}", "房间 {room} · {room_mode}")
                    .replace("{room_line2}", "人数 {members}/{room_max} · 房主 {owner} · {state}")
                    .replace("{sizes}", "A队 {team_a_size} 人 · B队 {team_b_size} 人");
            var matcher = java.util.regex.Pattern.compile("\\{[a-zA-Z0-9_:.]+\\}").matcher(template);
            java.util.List<String> pieces = new java.util.ArrayList<>();
            int end = 0;
            while (matcher.find()) {
                if (matcher.start() > end) pieces.add(template.substring(end, matcher.start()));
                pieces.add(matcher.group());
                end = matcher.end();
            }
            if (end < template.length()) pieces.add(template.substring(end));
            var font = Minecraft.getInstance().font;
            int total = pieces.stream().mapToInt(s -> pieceWidth(s, font)).sum();
            float fit = Math.min(1F, width / (float) Math.max(1, total));
            int left = x - Math.round(total * fit) / 2;
            int index = 0;
            for (String piece : pieces) {
                int partWidth = Math.max(1, Math.round(pieceWidth(piece, font) * fit));
                if (!piece.isBlank()) text(name + "·" + (++index), piece, left + partWidth / 2, y,
                        partWidth, color, Math.max(50, Math.round(fit * 100)));
                left += partWidth;
            }
        }

        private static int pieceWidth(String piece, net.minecraft.client.gui.Font font) {
            int width = font.width(HudParameters.render(piece, true)) + 4;
            if (!piece.startsWith("{") || !piece.endsWith("}")) return width;
            String key = piece.substring(1, piece.length() - 1);
            if (java.util.Set.of("killer", "victim", "room", "map", "owner", "player_name").contains(key)) return Math.max(96, width);
            var source = HudStats.byId(key);
            if (source != null && source.kind() != HudStats.Kind.TEXT) return Math.max(24, width);
            if (java.util.Set.of("target", "round", "members", "position", "queue", "ready", "room_max", "rounds_to_win").contains(key)) return Math.max(24, width);
            return width;
        }

        private void atom(String name, String type, String text, int x, int y, int width, int height,
                          int color, String source, int scale) {
            String base = example + "·" + name;
            String id = base;
            int suffix = 2;
            java.util.Set<String> ids = new java.util.HashSet<>();
            draft.customElements(context).forEach(e -> ids.add(e.id()));
            while (ids.contains(id)) id = base + "_" + suffix++;
            draft.addCustomElement(context, new CustomElement(id, type, text, anchorX, anchorY,
                    Math.max(1, width), Math.max(1, height), opacity, color, visible, source, scale,
                    type.equals("block"), false, false,
                    new Placement(x, y, 624, 344, example, condition)));
        }
    }
}
