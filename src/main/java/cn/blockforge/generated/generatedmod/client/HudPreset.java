package cn.blockforge.generated.generatedmod.client;

/** Built-in style presets use existing live HUD data and remain fully editable. */
public enum HudPreset {
    COMPETITIVE("综合竞技（推荐）", "完整展示阶段公告、比分、目标、个人、队伍、经济、生命护甲、武器弹药与 C4。"),
    COUNTER_STRIKE("反恐精英 CS2", "顶部紧凑比分、分队人数、金色状态元素。"),
    CROSSFIRE("穿越火线", "宽幅顶部比分，青色强调，醒目的击杀播报。"),
    CALL_OF_DUTY("使命召唤20 MWIII", "左上分队比分与进度、轻量战术信息。");

    private final String label;
    private final String description;

    HudPreset(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() { return label; }
    public String description() { return description; }

    public void apply(ClientHudLayout.Draft draft, HudContext context) {
        for (var e : java.util.List.copyOf(draft.customElements(context))) {
            if (!e.placement().example().isBlank()) draft.removeCustomElement(context, e.id());
        }
        for (var component : HudContext.BuiltIn.values()) draft.of(context).setBuiltInEnabled(component, false);
        int white = 0xFFF0F2F3, accent = this == COMPETITIVE ? 0xFF78D6A5
                : this == COUNTER_STRIKE ? 0xFFF0CE74
                : this == CROSSFIRE ? 0xFF88DDF1 : 0xFFD5E3A5;
        if (!context.isMatch()) {
            var b = new HudAssemblies.Builder(draft, context, label, 50, 0);
            b.opacity = 75;
            b.block("横幅底板", 0, 48, 360, 72, 0xFF181C20);
            b.block("横幅强调线", 0, 13, 360, 2, accent);
            if (context == HudContext.MATCHING) {
                b.line("匹配状态", "{matching_state}", 0, 29, 340, accent);
                b.condition = "queued";
                b.line("队列", "队列 {queue}/{queue_need}  序位 {position}", 0, 48, 340, white);
                b.line("时间", "已等待 {wait}", 0, 67, 340, white);
                b.condition = "forming";
                b.line("倒计时", "开赛倒计时 {ready} 秒", 0, 48, 340, white);
                b.line("入场提示", "准备进入比赛", 0, 67, 340, white);
            } else {
                b.line("房间", "{room} · {room_mode}", 0, 29, 340, accent);
                b.line("地图", "地图 {map} · {state}", 0, 48, 340, white);
                b.line("成员", "人数 {members}/{room_max} · 房主 {owner}", 0, 67, 340, white);
            }
            return;
        }
        boolean kills = context == HudContext.TEAM_DEATHMATCH;
        String score = kills ? "score_" : "wins_";
        var top = new HudAssemblies.Builder(draft, context, label, this == CALL_OF_DUTY ? 0 : 50, 0);
        int red = this == COUNTER_STRIKE ? 0xFFF0CE74 : 0xFFFF7078;
        int blue = 0xFF78C9F1;
        if (this == CALL_OF_DUTY) {
            top.text("模式", "{mode}", 87, 18, 150, white, 90);
            for (int i = 0; i < 4; i++) {
                String key = "abcd".substring(i, i + 1);
                top.condition = i < 2 ? "" : "team_" + key;
                int y = 40 + i * 24;
                int color = i == 0 ? blue : i == 1 ? red : i == 2 ? 0xFF8BDCA0 : accent;
                top.text("队名" + key, key.toUpperCase() + "队", 27, y, 32, color, 90);
                top.text("比分" + key, "{" + score + key + "}", 66, y, 36, color, 160);
                top.progress("得分进度" + key, "team_" + key + (kills ? "_progress" : "_wins_progress"),
                        106, y + 10, 180, color);
            }
            top.condition = "";
            top.line("计时", "{time}  回合 {round}", 106, 138, 180, white);
            top.line("目标", kills ? "目标 {target}" : "先胜 {rounds_to_win} 回合", 106, 154, 180, accent);
        } else {
            boolean cs = this == COUNTER_STRIKE || this == COMPETITIVE;
            int wing = this == COMPETITIVE ? 76 : cs ? 65 : 104;
            top.opacity = cs ? 65 : 85;
            top.block("中央计时底板", 0, 31, cs ? 60 : 84, 42, 0xFF15181C);
            top.text("计时", "{time}", 0, 22, cs ? 56 : 76, white, 110);
            top.text("回合数", "{round}", 0, 41, 30, accent, 110);
            top.text("回合标签", "回合", 0, 57, 40, white, 75);
            for (int i = 0; i < 4; i++) {
                String key = "abcd".substring(i, i + 1);
                top.condition = i < 2 ? "" : "team_" + key;
                int x = i % 2 == 0 ? -wing : wing;
                int y = i < 2 ? 31 : 86;
                int color = i == 0 ? red : i == 1 ? blue : i == 2 ? 0xFF8BDCA0 : 0xFFEECF79;
                top.block("队伍底板" + key, x, y, cs ? 62 : 118, 42, 0xFF22282E);
                top.block("队伍色条" + key, x, y - 20, cs ? 62 : 118, 2, color);
                top.text("队名" + key, key.toUpperCase() + "队", x - (cs ? 18 : 37), y - 9, cs ? 24 : 28, color, 80);
                top.text("比分" + key, "{" + score + key + "}", x + (cs ? 15 : 22), y - 3, cs ? 32 : 54, color, cs ? 150 : 190);
                top.line("人数" + key, "{team_" + key + "_size} 人", x - (cs ? 8 : 34), y + 14, cs ? 42 : 54, white);
            }
            top.condition = "";
            top.line("规则", kills ? "{mode} · 目标 {target}" : "{mode} · 先胜 {rounds_to_win} 回合",
                    0, 123, 300, white);
        }
        var status = new HudAssemblies.Builder(draft, context, label, 0, 100);
        status.opacity = 72;
        status.block("状态底板", 130, -68, 260, 88, 0xFF11161B);
        status.text("生命标签", "HP", 30, -86, 32, accent, 100);
        status.text("生命值", "{health}", 72, -86, 44, white, 160);
        status.text("护甲标签", "AP", 125, -86, 30, blue, 100);
        status.text("护甲值", "{armor}", 165, -86, 44, white, 140);
        status.progress("生命条", "health_percent", 130, -71, 230, accent);
        status.line("武器与弹药", "{held_weapon}  ·  {held_ammo}", 130, -51, 230, white);
        status.progress("武器耐久", "held_durability", 130, -37, 230, red);
        var personal = new HudAssemblies.Builder(draft, context, label, 100, 100);
        personal.opacity = 72;
        personal.block("个人底板", -135, -69, 270, 100, 0xFF11161B);
        personal.line("击杀统计", "击杀 {my_kills}  阵亡 {my_deaths}  KD {my_kd}",
                -130, -86, 260, white);
        personal.line("输出统计", "输出 {my_damage}  承伤 {my_damage_taken}",
                -130, -68, 260, white);
        personal.line("队伍与经济", "{team} · 队伍击杀 {match_kills_sum} · 资金 {my_match_money}",
                -130, -50, 260, accent);
        var feed = new HudAssemblies.Builder(draft, context, label, 100, 45);
        feed.condition = "feed";
        feed.opacity = 80;
        feed.block("击杀背景", -119, 0, 220, 20, 0xFF16181B);
        feed.line("击杀播报", "{killer} > {victim}", -119, 0, 208, white);
        addMatchReadouts(draft, context, label, accent, white, red);
    }

    private static void addMatchReadouts(ClientHudLayout.Draft draft, HudContext context,
                                         String example, int accent, int white, int red) {
        var notice = new HudAssemblies.Builder(draft, context, example, 50, 42);
        notice.opacity = 80;
        notice.condition = "notice";
        notice.block("阶段公告底板", 0, 0, 340, 48, 0xFF11161B);
        notice.block("阶段公告强调线", 0, -23, 340, 2, accent);
        notice.text("阶段公告标题", "{notice_title}", -95, -8, 130, accent, 110);
        notice.text("阶段公告计时", "{notice_timer}", 120, -8, 68, white, 105);
        notice.line("阶段公告详情", "{notice_detail}", 0, 10, 320, white);

        var bomb = new HudAssemblies.Builder(draft, context, example, 50, 53);
        bomb.opacity = 88;
        bomb.condition = "c4_active";
        bomb.text("C4状态", "{bomb_phase} · {bomb_site}", 0, -9, 300, red, 90);
        bomb.progress("C4倒计时", "bomb_countdown", 0, 0, 300, red);
    }

    public void apply(ClientHudLayout.Mutable v, HudContext context) {
        for (HudContext.BuiltIn component : HudContext.BuiltIn.values()) {
            v.setBuiltInEnabled(component, true);
        }
        v.scoreVisible = v.textVisible = v.feedVisible = context.isMatch();
        v.bannerVisible = !context.isMatch();
        v.scoreXPercent = 50; v.scoreYPercent = 2;
        v.textXPercent = 50; v.textYPercent = 78; v.textWidth = 220;
        v.feedXPercent = 100; v.feedYPercent = 42;
        v.bannerXPercent = 50; v.bannerYPercent = 12;
        v.scoreHeaderTemplate = "{mode} · {phase}";
        v.scoreTeamATemplate = "A队 {score_a}";
        v.scoreTeamBTemplate = "B队 {score_b}";
        v.scoreTimerTemplate = "{time}";
        v.scoreDetailsTemplate = "回合 {round}    目标 {target}";
        v.textTemplate = "{hint}";
        v.bannerLineOneTemplate = context == HudContext.ROOM ? "{room_line1}" : "{matching_line1}";
        v.bannerLineTwoTemplate = context == HudContext.ROOM ? "{room_line2}" : "{matching_line2}";
        switch (this) {
            case COMPETITIVE -> {
                v.scoreWidth = 340; v.scoreScalePercent = 92; v.scoreOpacityPercent = 78;
                v.textScalePercent = 92; v.textOpacityPercent = 45;
                v.feedScalePercent = 92; v.feedOpacityPercent = 72;
                v.bannerScalePercent = 92; v.bannerOpacityPercent = 78;
                v.scoreColor = v.bannerColor = 0x78D6A5;
                v.textColor = 0xE8ECEF; v.feedColor = 0xF0CE74;
                v.feedTemplate = "{killer} 击杀 {victim}";
            }
            case COUNTER_STRIKE -> {
                v.scoreWidth = 280; v.scoreScalePercent = 85; v.scoreOpacityPercent = 65;
                v.textXPercent = 0; v.textScalePercent = 85; v.textOpacityPercent = 35;
                v.feedScalePercent = 85; v.feedOpacityPercent = 65;
                v.bannerScalePercent = 85; v.bannerOpacityPercent = 65;
                v.scoreColor = v.textColor = v.bannerColor = 0xE5C56C;
                v.feedColor = 0xE8E8E8;
                v.feedTemplate = "{killer} > {victim}";
            }
            case CROSSFIRE -> {
                v.scoreWidth = 400; v.scoreScalePercent = 100; v.scoreOpacityPercent = 90;
                v.textScalePercent = 100; v.textOpacityPercent = 75;
                v.feedScalePercent = 100; v.feedOpacityPercent = 85;
                v.bannerScalePercent = 100; v.bannerOpacityPercent = 90;
                v.scoreColor = v.bannerColor = 0x67D4ED;
                v.textColor = 0xF0F0F0; v.feedColor = 0xF0CE70;
                v.feedTemplate = "{killer} 击杀 {victim}";
            }
            case CALL_OF_DUTY -> {
                v.scoreXPercent = 0; v.scoreWidth = 240;
                v.scoreScalePercent = 85; v.scoreOpacityPercent = 45;
                v.textXPercent = 0; v.textScalePercent = 85; v.textOpacityPercent = 25;
                v.feedScalePercent = 85; v.feedOpacityPercent = 40;
                v.bannerXPercent = 0; v.bannerScalePercent = 85; v.bannerOpacityPercent = 45;
                v.scoreColor = v.feedColor = 0xF1F3F2;
                v.textColor = v.bannerColor = 0xA9CF80;
                v.feedTemplate = "{killer} / {victim}";
            }
        }
    }
}
