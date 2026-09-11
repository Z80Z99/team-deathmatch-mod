package cn.blockforge.generated.generatedmod.weapon;

public record WeaponRepositoryItem(String id, String categoryId, WeaponSnapshot snapshot,
                                   String label, boolean enabled) {
    public WeaponRepositoryItem withEnabled(boolean value) {
        return new WeaponRepositoryItem(id, categoryId, snapshot, label, value);
    }

    public String title() {
        return label == null || label.isBlank() ? snapshot.displayName() : label;
    }
}
