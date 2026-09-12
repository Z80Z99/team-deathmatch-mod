package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.ClientHudLayout.CustomElement;
import cn.blockforge.generated.generatedmod.client.ClientHudLayout.Elements;
import cn.blockforge.generated.generatedmod.client.ClientHudLayout.Snapshot;
import cn.blockforge.generated.generatedmod.match.GameMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 对其他玩法开放的 HUD 客户端 API。
 *
 * <p>所有布局都按场景保存：比赛按 {@link GameMode} 分开，另外提供正在匹配和房间中场景。
 * 该 API 不强制任何内置组件常驻；第三方可以关闭它们，再添加自己的模块或渲染类型。
 */
public final class HudApi {
    @FunctionalInterface
    public interface BuiltInRenderer {
        /** 返回 true 表示已接管绘制，默认 HUD 将跳过该组件。 */
        boolean render(GuiGraphics graphics, Font font, HudContext context,
                       HudContext.BuiltIn component, Elements elements, int width, int height,
                       float partialTick);
    }

    private static final Map<String, BuiltInRenderer> BUILT_IN_RENDERERS = new LinkedHashMap<>();

    private HudApi() {
    }

    public static List<HudStats.Source> sources() {
        return HudStats.sourcesWithDynamic();
    }

    public static List<HudStats.Source> sources(HudContext context) {
        return HudStats.sourcesFor(context);
    }

    public static void registerCondition(String id, BooleanSupplier condition) { HudConditions.register(id, condition); }
    public static boolean unregisterCondition(String id) { return HudConditions.unregister(id); }

    public static void registerEvent(String id, String title, String defaultDetail,
                                     int durationTicks, int priority) {
        ClientHudEventData.register(new ClientHudEventData.Descriptor(id, title, defaultDetail,
                durationTicks, priority));
    }

    public static void registerEvent(String id, String title, String defaultDetail,
                                     int durationTicks, int priority,
                                     java.util.Set<HudContext> contexts) {
        ClientHudEventData.register(new ClientHudEventData.Descriptor(id, title, defaultDetail,
                durationTicks, priority, contexts));
    }

    public static boolean unregisterEvent(String id) {
        return ClientHudEventData.unregister(id);
    }

    public static void registerSource(HudStats.Source source) {
        HudStats.register(source);
    }

    public static void registerNumber(String id, String name, HudStats.Group group,
                                      DoubleSupplier value, BooleanSupplier live, double demo) {
        registerSource(new HudStats.Source(id, name, group, HudStats.Kind.NUMBER, value,
                () -> 0, () -> "", live, demo, 0, ""));
    }

    public static void registerNumber(String id, String name, HudStats.Group group,
                                      DoubleSupplier value, BooleanSupplier live, double demo,
                                      java.util.Set<HudContext> contexts) {
        registerSource(new HudStats.Source(id, name, group, HudStats.Kind.NUMBER, value,
                () -> 0, () -> "", live, demo, 0, "", contexts));
    }

    public static void registerText(String id, String name, HudStats.Group group,
                                    Supplier<String> text, BooleanSupplier live, String demoText) {
        registerSource(new HudStats.Source(id, name, group, HudStats.Kind.TEXT, () -> 0,
                () -> 0, text, live, 0, 0, demoText));
    }

    public static void registerText(String id, String name, HudStats.Group group,
                                    Supplier<String> text, BooleanSupplier live, String demoText,
                                    java.util.Set<HudContext> contexts) {
        registerSource(new HudStats.Source(id, name, group, HudStats.Kind.TEXT, () -> 0,
                () -> 0, text, live, 0, 0, demoText, contexts));
    }

    public static void registerProgress(String id, String name, HudStats.Group group,
                                        DoubleSupplier value, IntSupplier maximum,
                                        BooleanSupplier live, double demoValue, int demoMaximum) {
        registerSource(new HudStats.Source(id, name, group, HudStats.Kind.PROGRESS, value,
                maximum, () -> "", live, demoValue, demoMaximum, ""));
    }

    public static void registerProgress(String id, String name, HudStats.Group group,
                                        DoubleSupplier value, IntSupplier maximum,
                                        BooleanSupplier live, double demoValue, int demoMaximum,
                                        java.util.Set<HudContext> contexts) {
        registerSource(new HudStats.Source(id, name, group, HudStats.Kind.PROGRESS, value,
                maximum, () -> "", live, demoValue, demoMaximum, "", contexts));
    }

    public static boolean unregisterSource(String id) {
        return HudStats.unregister(id);
    }

    public static void registerRenderer(String type, HudCustomRenderer.ElementRenderer renderer) {
        HudCustomRenderer.registerRenderer(type, renderer);
    }

    public static boolean unregisterRenderer(String type) {
        return HudCustomRenderer.unregisterRenderer(type);
    }

    public static synchronized void registerBuiltInRenderer(HudContext context,
                                                             HudContext.BuiltIn component,
                                                             BuiltInRenderer renderer) {
        if (context == null || component == null || renderer == null) {
            throw new IllegalArgumentException("HUD 内置渲染器参数无效");
        }
        BUILT_IN_RENDERERS.put(key(context, component), renderer);
    }

    public static synchronized boolean unregisterBuiltInRenderer(HudContext context,
                                                                 HudContext.BuiltIn component) {
        return context != null && component != null && BUILT_IN_RENDERERS.remove(key(context, component)) != null;
    }

    public static synchronized BuiltInRenderer builtInRenderer(HudContext context,
                                                               HudContext.BuiltIn component) {
        return context == null || component == null ? null : BUILT_IN_RENDERERS.get(key(context, component));
    }

    private static String key(HudContext context, HudContext.BuiltIn component) {
        return context.key() + "#" + component.name();
    }

    public static Snapshot layout() {
        return ClientHudLayout.snapshot();
    }

    public static Elements elements(HudContext context) {
        return ClientHudLayout.elements(context);
    }

    public static List<CustomElement> customElements(HudContext context) {
        return ClientHudLayout.customElements(context);
    }

    public static void setBuiltInVisible(HudContext context, HudContext.BuiltIn component, boolean visible) {
        if (context == null || component == null) {
            throw new IllegalArgumentException("HUD 内置组件参数无效");
        }
        Snapshot snapshot = ClientHudLayout.snapshot();
        ClientHudLayout.Draft draft = snapshot.draft();
        ClientHudLayout.Mutable mutable = draft.of(context);
        String exampleName = "示例·" + component.displayName();
        for (CustomElement element : List.copyOf(draft.customElements(context))) {
            if (exampleName.equals(element.placement().example())) {
                draft.removeCustomElement(context, element.id());
            }
        }
        mutable.setBuiltInEnabled(component, visible);
        switch (component) {
            case SCORE -> mutable.scoreVisible = visible;
            case NOTICE, TEXT -> mutable.textVisible = visible;
            case FEED -> mutable.feedVisible = visible;
            case BANNER -> mutable.bannerVisible = visible;
        }
        ClientHudLayout.apply(draft.build());
    }

    public static void add(HudContext context, CustomElement element) {
        if (context == null || element == null) {
            throw new IllegalArgumentException("HUD 模块参数无效");
        }
        Snapshot snapshot = ClientHudLayout.snapshot();
        ClientHudLayout.Draft draft = snapshot.draft();
        draft.addCustomElement(context, element);
        ClientHudLayout.apply(draft.build());
    }

    public static boolean remove(HudContext context, String id) {
        if (context == null || id == null || id.isBlank()) return false;
        Snapshot snapshot = ClientHudLayout.snapshot();
        ClientHudLayout.Draft draft = snapshot.draft();
        boolean exists = draft.customElements(context).stream().anyMatch(element -> element.id().equals(id));
        if (exists) {
            draft.removeCustomElement(context, id);
            ClientHudLayout.apply(draft.build());
        }
        return exists;
    }

    /** Replace an existing module by ID, or add it when the ID is new. */
    public static void update(HudContext context, CustomElement element) {
        if (context == null || element == null) {
            throw new IllegalArgumentException("HUD 模块参数无效");
        }
        Snapshot snapshot = ClientHudLayout.snapshot();
        ClientHudLayout.Draft draft = snapshot.draft();
        draft.replaceCustomElement(context, element);
        ClientHudLayout.apply(draft.build());
    }

    public static String save() {
        return ClientHudLayout.saveNow();
    }

    public static String resolve(String template, Map<String, String> values) {
        return HudStats.resolveTemplate(template, values);
    }
}
