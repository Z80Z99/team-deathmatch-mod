package cn.blockforge.generated.generatedmod.match;

import net.minecraft.ChatFormatting;

public enum Team {
    TEAM_A("team_a", "A队", ChatFormatting.RED, 0xFFE05555),
    TEAM_B("team_b", "B队", ChatFormatting.BLUE, 0xFF5599FF),
    SPECTATOR("spectator", "观战", ChatFormatting.GRAY, 0xFFB0B0B0),
    TEAM_C("team_c", "C队", ChatFormatting.GREEN, 0xFF78CC88),
    TEAM_D("team_d", "D队", ChatFormatting.YELLOW, 0xFFF0CF68);

    public static java.util.List<Team> playing(int count) {
        return java.util.List.of(TEAM_A, TEAM_B, TEAM_C, TEAM_D).subList(0, Math.max(2, Math.min(4, count)));
    }

    private final String key;
    private final String displayName;
    private final ChatFormatting chatColor;
    private final int hudColor;

    Team(String key, String displayName, ChatFormatting chatColor, int hudColor) {
        this.key = key;
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.hudColor = hudColor;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public ChatFormatting chatColor() {
        return chatColor;
    }

    public int hudColor() {
        return hudColor;
    }

    public boolean isPlayable() {
        return this != SPECTATOR;
    }

    public Team opposite() {
        return switch (this) {
            case TEAM_A -> TEAM_B;
            case TEAM_B -> TEAM_A;
            case SPECTATOR, TEAM_C, TEAM_D -> SPECTATOR;
        };
    }

    public static Team parse(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "a", "team_a", "teama", "red" -> TEAM_A;
            case "b", "team_b", "teamb", "blue" -> TEAM_B;
            case "c", "team_c", "teamc", "green" -> TEAM_C;
            case "d", "team_d", "teamd", "yellow" -> TEAM_D;
            case "spectator", "spectate", "观战" -> SPECTATOR;
            default -> null;
        };
    }
}
