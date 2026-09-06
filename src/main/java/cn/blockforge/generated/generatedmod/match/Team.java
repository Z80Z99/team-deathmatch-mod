package cn.blockforge.generated.generatedmod.match;

import net.minecraft.ChatFormatting;

public enum Team {
    TEAM_A("team_a", "A队", ChatFormatting.RED, 0xFFE05555),
    TEAM_B("team_b", "B队", ChatFormatting.BLUE, 0xFF5599FF),
    SPECTATOR("spectator", "观战", ChatFormatting.GRAY, 0xFFB0B0B0);

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
        return this == TEAM_A || this == TEAM_B;
    }

    public Team opposite() {
        return switch (this) {
            case TEAM_A -> TEAM_B;
            case TEAM_B -> TEAM_A;
            case SPECTATOR -> SPECTATOR;
        };
    }

    public static Team parse(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "a", "team_a", "teama", "red" -> TEAM_A;
            case "b", "team_b", "teamb", "blue" -> TEAM_B;
            case "spectator", "spectate", "观战" -> SPECTATOR;
            default -> null;
        };
    }
}
