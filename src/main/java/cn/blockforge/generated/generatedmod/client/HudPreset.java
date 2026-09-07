package cn.blockforge.generated.generatedmod.client;

/** Built-in style presets use existing live HUD data and remain fully editable. */
public enum HudPreset {
    COUNTER_STRIKE("反恐精英", "紧凑顶部比分，金色强调，低透明度状态条。"),
    CROSSFIRE("穿越火线", "宽幅顶部比分，青色强调，醒目的击杀播报。"),
    CALL_OF_DUTY("使命召唤", "左上战术比分，白色与绿色强调，轻量信息显示。");

    private final String label;
    private final String description;

    HudPreset(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() { return label; }
    public String description() { return description; }

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
        v.scoreDetailsTemplate = "{round}    {target}";
        v.textTemplate = "{hint}";
        v.bannerLineOneTemplate = context == HudContext.ROOM ? "{room_line1}" : "{matching_line1}";
        v.bannerLineTwoTemplate = context == HudContext.ROOM ? "{room_line2}" : "{matching_line2}";
        switch (this) {
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
