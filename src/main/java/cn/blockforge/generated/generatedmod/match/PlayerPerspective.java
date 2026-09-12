package cn.blockforge.generated.generatedmod.match;

public enum PlayerPerspective {
    FIRST_PERSON("第一人称"),
    THIRD_PERSON_BACK("背后第三人称");

    private final String displayName;

    PlayerPerspective(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
