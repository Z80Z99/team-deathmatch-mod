package cn.blockforge.generated.generatedmod.client;

import java.util.*;
import java.util.function.BooleanSupplier;

public final class HudConditions {
    private static final Map<String, BooleanSupplier> EXTERNAL = new LinkedHashMap<>();
    private HudConditions() { }
    public static void register(String id, BooleanSupplier condition) {
        if (id == null || !id.startsWith("api:") || condition == null) throw new IllegalArgumentException("Use api: condition IDs");
        EXTERNAL.put(id, condition);
    }
    public static boolean unregister(String id) { return EXTERNAL.remove(id) != null; }
    public static Set<String> ids() { return Set.copyOf(EXTERNAL.keySet()); }
    static Boolean external(String id) {
        var condition = EXTERNAL.get(id);
        if (condition == null) return null;
        try { return condition.getAsBoolean(); } catch (RuntimeException error) { return false; }
    }
    public static boolean visible(ClientHudLayout.CustomElement element,
                                  List<ClientHudLayout.CustomElement> elements, boolean editor) {
        return Boolean.TRUE.equals(evaluate(element, elements, editor, new HashSet<>()));
    }
    private static Boolean evaluate(ClientHudLayout.CustomElement element,
                                   List<ClientHudLayout.CustomElement> elements, boolean editor, Set<String> path) {
        if (editor) return element.visible();
        if (!path.add(element.id())) return null;
        if (!element.visible()) return false;
        String condition = element.placement().condition();
        if (!condition.startsWith("hidden:")) return HudParameters.visible(condition, false);
        String targetId = condition.substring(7);
        if (path.contains(targetId)) return null;
        var target = elements.stream().filter(e -> e.id().equals(targetId)).findFirst();
        if (target.isEmpty()) return true;
        Boolean showing = evaluate(target.get(), elements, false, path);
        if (showing == null) return null;
        return !showing && HudAnimation.disappeared(targetId);
    }
}
