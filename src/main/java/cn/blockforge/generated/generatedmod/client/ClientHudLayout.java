package cn.blockforge.generated.generatedmod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端 HUD 配置：每名玩家独立保存在本机 config/fpsmod/client-hud.json，
 * 不上传服务器；该文件也可以在游戏外直接编辑（外部自定义）。
 *
 * <p>配置按 {@link HudContext 场景}分开：团队死斗 / 爆破 / 歼灭三份比赛配置，
 * 外加“正在匹配”“房间中”两个大厅场景；每个场景内再细分元素
 * （比分记录条、状态文字、击杀播报、场景横幅）与全局的背景图 / 参考线。
 */
public final class ClientHudLayout {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_hud_layout");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 5;
    private static final int SCORE_BIT = 1;
    private static final int TEXT_BIT = 1 << 1;
    private static final int FEED_BIT = 1 << 2;
    private static final int BANNER_BIT = 1 << 3;
    private static final int ALL_BUILT_IN_BITS = SCORE_BIT | TEXT_BIT | FEED_BIT | BANNER_BIT;

    private static int builtInBit(HudContext.BuiltIn component) {
        return switch (component) {
            case SCORE -> SCORE_BIT;
            case TEXT -> TEXT_BIT;
            case FEED -> FEED_BIT;
            case BANNER -> BANNER_BIT;
        };
    }

    private static final EnumMap<HudContext, Elements> CONTEXTS = new EnumMap<>(HudContext.class);
    private static final EnumMap<HudContext, List<CustomElement>> CUSTOM_ELEMENTS = new EnumMap<>(HudContext.class);
    private static Global global = Global.defaults();
    private static boolean loaded;

    static {
        for (HudContext context : HudContext.values()) {
            CONTEXTS.put(context, Elements.defaultsFor(context));
            CUSTOM_ELEMENTS.put(context, new ArrayList<>());
        }
    }

    private ClientHudLayout() {
    }

    /** 玩家放入此目录的 PNG 可作为 HUD 背景参考图。 */
    public static Path backgroundDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("fpsmod").resolve("hud_images");
    }

    /** 玩家可直接编辑的配置文件路径（外部自定义入口）。 */
    public static Path configFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("fpsmod").resolve("client-hud.json");
    }

    public static synchronized Snapshot snapshot() {
        ensureLoaded();
        return new Snapshot(global, CONTEXTS, CUSTOM_ELEMENTS);
    }

    public static synchronized List<CustomElement> customElements(HudContext context) {
        ensureLoaded();
        return List.copyOf(CUSTOM_ELEMENTS.getOrDefault(context, List.of()));
    }

    public static synchronized Elements elements(HudContext context) {
        ensureLoaded();
        return CONTEXTS.getOrDefault(context, Elements.defaultsFor(context));
    }

    public static synchronized Global global() {
        ensureLoaded();
        return global;
    }

    /** 当前比赛模式对应的配置场景；不在比赛中返回 null。 */
    public static HudContext activeMatchContext() {
        return ClientMatchData.inMatch() ? HudContext.match(ClientMatchData.mode) : null;
    }

    /** 实时预览用的内存更新（不写盘）；松手或关窗后用 saveNow 持久化。 */
    public static synchronized String updateTransient(Snapshot updated) {
        ensureLoaded();
        String error = updated == null ? "HUD 配置为空。" : updated.validationError();
        if (error != null) {
            return error;
        }
        global = updated.global();
        for (HudContext context : HudContext.values()) {
            CONTEXTS.put(context, updated.elements(context));
            CUSTOM_ELEMENTS.put(context, new ArrayList<>(updated.customElements(context)));
        }
        return null;
    }

    /** 应用并立即写盘；返回 null 表示成功，否则返回可显示给用户的错误。 */
    public static synchronized String apply(Snapshot updated) {
        String error = updateTransient(updated);
        if (error != null) {
            return error;
        }
        return save();
    }

    /** 立即写盘；返回 null 表示成功，否则返回错误文本。 */
    public static synchronized String saveNow() {
        ensureLoaded();
        return save();
    }

    public static synchronized Snapshot resetDefaults() {
        global = Global.defaults();
        for (HudContext context : HudContext.values()) {
            CONTEXTS.put(context, Elements.defaultsFor(context));
            CUSTOM_ELEMENTS.put(context, new ArrayList<>());
        }
        loaded = true;
        save();
        return snapshot();
    }

    // ------------------------------------------------------------- 持久化

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = configFile();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            if (object.has("contexts")) {
                readVersion2(object);
            } else {
                migrateVersion1(object);
            }
        } catch (Exception error) {
            LOGGER.warn("读取客户端 HUD 配置失败，将使用默认值：{}", file, error);
        }
    }

    private static void readVersion2(JsonObject object) {
        if (object.has("global") && object.get("global").isJsonObject()) {
            Global parsed = Global.read(object.getAsJsonObject("global"));
            if (parsed.validationError() == null) {
                global = parsed;
            }
        }
        JsonObject contexts = object.getAsJsonObject("contexts");
        for (Map.Entry<String, com.google.gson.JsonElement> entry : contexts.entrySet()) {
            HudContext context = HudContext.fromKey(entry.getKey());
            if (context == null || !entry.getValue().isJsonObject()) {
                continue;
            }
            Elements parsed = Elements.read(entry.getValue().getAsJsonObject(),
                    Elements.defaultsFor(context));
            if (parsed.validationError() == null) {
                CONTEXTS.put(context, parsed);
            }
        }
        if (object.has("customElements") && object.get("customElements").isJsonObject()) {
            JsonObject custom = object.getAsJsonObject("customElements");
            for (Map.Entry<String, com.google.gson.JsonElement> entry : custom.entrySet()) {
                HudContext context = HudContext.fromKey(entry.getKey());
                if (context == null || !entry.getValue().isJsonArray()) continue;
                List<CustomElement> parsed = new ArrayList<>();
                for (com.google.gson.JsonElement value : entry.getValue().getAsJsonArray()) {
                    if (!value.isJsonObject()) continue;
                    CustomElement element = CustomElement.read(value.getAsJsonObject());
                    if (element.validationError() == null) parsed.add(element);
                }
                CUSTOM_ELEMENTS.put(context, parsed);
            }
        }
    }

    /** 旧版（单场景扁平字段）自动升级：三份比赛场景沿用旧配置，大厅场景用默认值。 */
    private static void migrateVersion1(JsonObject object) {
        Elements legacy = new Elements(
                bool(object, "scoreVisible", true),
                integer(object, "scoreXPercent", 50),
                integer(object, "scoreYPercent", 2),
                integer(object, "scoreWidth", 340),
                integer(object, "scoreScalePercent", 100),
                integer(object, "scoreOpacityPercent", 80, 20, 100),
                bool(object, "hintVisible", true),
                integer(object, "hintXPercent", 50),
                integer(object, "hintYPercent", 92),
                integer(object, "hintWidth", 220),
                integer(object, "hintScalePercent", 100),
                integer(object, "hintOpacityPercent", 80, 20, 100),
                bool(object, "killFeedVisible", true),
                50, 16, 100, 85,
                false, 50, 30, 100, 85);
        if (legacy.validationError() != null) {
            legacy = Elements.defaultsFor(HudContext.TEAM_DEATHMATCH);
        }
        CONTEXTS.put(HudContext.TEAM_DEATHMATCH, legacy);
        CONTEXTS.put(HudContext.SEARCH_DESTROY, legacy);
        CONTEXTS.put(HudContext.LAST_STANDING, legacy);
        global = new Global(
                bool(object, "guidesVisible", true),
                string(object, "backgroundFile", ""),
                integer(object, "backgroundOpacityPercent", 60, 0, 100));
    }

    private static String save() {
        Path file = configFile();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            JsonObject object = new JsonObject();
            object.addProperty("version", VERSION);
            object.add("global", global.toJson());
            JsonObject contexts = new JsonObject();
            for (HudContext context : HudContext.values()) {
                contexts.add(context.key(), CONTEXTS.get(context).toJson());
            }
            object.add("contexts", contexts);
            com.google.gson.JsonObject custom = new com.google.gson.JsonObject();
            for (HudContext context : HudContext.values()) {
                com.google.gson.JsonArray array = new com.google.gson.JsonArray();
                for (CustomElement element : CUSTOM_ELEMENTS.getOrDefault(context, List.of())) {
                    array.add(element.toJson());
                }
                custom.add(context.key(), array);
            }
            object.add("customElements", custom);
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(object, writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return null;
        } catch (Exception error) {
            LOGGER.error("保存客户端 HUD 配置失败：{}", file, error);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                LOGGER.debug("清理临时 HUD 配置文件失败：{}", temporary, cleanupError);
            }
            return "保存 HUD 配置失败，请检查 config/fpsmod 目录权限。";
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
    }

    private static int integer(JsonObject object, String key, int fallback, int minimum, int maximum) {
        int value = integer(object, key, fallback);
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBoolean() : fallback;
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    // ------------------------------------------------------------- 数据模型

    /** 一个场景内的全部可调元素；横幅字段只对大厅场景生效，其余只对比赛场景生效。 */
    public record Elements(
            boolean scoreVisible, int scoreXPercent, int scoreYPercent, int scoreWidth,
            int scoreScalePercent, int scoreOpacityPercent,
            boolean textVisible, int textXPercent, int textYPercent, int textWidth,
            int textScalePercent, int textOpacityPercent,
            boolean feedVisible, int feedXPercent, int feedYPercent,
            int feedScalePercent, int feedOpacityPercent,
            boolean bannerVisible, int bannerXPercent, int bannerYPercent,
            int bannerScalePercent, int bannerOpacityPercent,
            String scoreHeaderTemplate, String scoreTeamATemplate, String scoreTeamBTemplate,
            String scoreTimerTemplate, String scoreDetailsTemplate, String textTemplate,
            String feedTemplate, String bannerLineOneTemplate, String bannerLineTwoTemplate,
            int scoreColor, int textColor, int feedColor, int bannerColor, int builtInMask) {
        public Elements {
            builtInMask &= ALL_BUILT_IN_BITS;
        }

        /** 兼容上一版完整模板构造：旧配置默认保留所有内置组件。 */
        public Elements(boolean scoreVisible, int scoreXPercent, int scoreYPercent, int scoreWidth,
                        int scoreScalePercent, int scoreOpacityPercent,
                        boolean textVisible, int textXPercent, int textYPercent, int textWidth,
                        int textScalePercent, int textOpacityPercent,
                        boolean feedVisible, int feedXPercent, int feedYPercent,
                        int feedScalePercent, int feedOpacityPercent,
                        boolean bannerVisible, int bannerXPercent, int bannerYPercent,
                        int bannerScalePercent, int bannerOpacityPercent,
                        String scoreHeaderTemplate, String scoreTeamATemplate, String scoreTeamBTemplate,
                        String scoreTimerTemplate, String scoreDetailsTemplate, String textTemplate,
                        String feedTemplate, String bannerLineOneTemplate, String bannerLineTwoTemplate,
                        int scoreColor, int textColor, int feedColor, int bannerColor) {
            this(scoreVisible, scoreXPercent, scoreYPercent, scoreWidth, scoreScalePercent, scoreOpacityPercent,
                    textVisible, textXPercent, textYPercent, textWidth, textScalePercent, textOpacityPercent,
                    feedVisible, feedXPercent, feedYPercent, feedScalePercent, feedOpacityPercent,
                    bannerVisible, bannerXPercent, bannerYPercent, bannerScalePercent, bannerOpacityPercent,
                    scoreHeaderTemplate, scoreTeamATemplate, scoreTeamBTemplate, scoreTimerTemplate,
                    scoreDetailsTemplate, textTemplate, feedTemplate, bannerLineOneTemplate,
                    bannerLineTwoTemplate, scoreColor, textColor, feedColor, bannerColor, ALL_BUILT_IN_BITS);
        }

        /** 兼容旧版调用：内容模板与颜色使用默认值。 */
        public Elements(boolean scoreVisible, int scoreXPercent, int scoreYPercent, int scoreWidth,
                        int scoreScalePercent, int scoreOpacityPercent,
                        boolean textVisible, int textXPercent, int textYPercent, int textWidth,
                        int textScalePercent, int textOpacityPercent,
                        boolean feedVisible, int feedXPercent, int feedYPercent,
                        int feedScalePercent, int feedOpacityPercent,
                        boolean bannerVisible, int bannerXPercent, int bannerYPercent,
                        int bannerScalePercent, int bannerOpacityPercent) {
            this(scoreVisible, scoreXPercent, scoreYPercent, scoreWidth, scoreScalePercent, scoreOpacityPercent,
                    textVisible, textXPercent, textYPercent, textWidth, textScalePercent, textOpacityPercent,
                    feedVisible, feedXPercent, feedYPercent, feedScalePercent, feedOpacityPercent,
                    bannerVisible, bannerXPercent, bannerYPercent, bannerScalePercent, bannerOpacityPercent,
                    "{mode} · {phase}", "A队 {score_a}", "B队 {score_b}", "{time}",
                    "{round}    {target}", "{hint}", "⚔ {killer} 击杀 {victim}",
                    "{matching_line1}", "{matching_line2}", UiTheme.ACCENT, UiTheme.ACCENT,
                    UiTheme.ACCENT, UiTheme.ACCENT);
        }

        public static Elements defaultsFor(HudContext context) {
            return switch (context) {
                case TEAM_DEATHMATCH, SEARCH_DESTROY, LAST_STANDING -> new Elements(
                        true, 50, 2, 340, 100, 80,
                        true, 50, 78, 220, 100, 80,
                        true, 80, 42, 100, 85,
                        false, 50, 30, 100, 85);
                case MATCHING -> new Elements(
                        false, 50, 2, 340, 100, 80,
                        false, 50, 92, 220, 100, 80,
                        false, 50, 16, 100, 85,
                        true, 50, 30, 100, 85).withTemplates(null, null, null, null, null, null, null,
                        "{matching_line1}", "{matching_line2}");
                case ROOM -> new Elements(
                        false, 50, 2, 340, 100, 80,
                        false, 50, 92, 220, 100, 80,
                        false, 50, 16, 100, 85,
                        true, 50, 12, 100, 85).withTemplates(null, null, null, null, null, null, null,
                        "{room_line1}", "{room_line2}");
            };
        }

        public String validationError() {
            if (!between(scoreXPercent, 0, 100) || !between(scoreYPercent, 0, 100)
                    || !between(textXPercent, 0, 100) || !between(textYPercent, 0, 100)
                    || !between(feedXPercent, 0, 100) || !between(feedYPercent, 0, 100)
                    || !between(bannerXPercent, 0, 100) || !between(bannerYPercent, 0, 100)) {
                return "HUD 位置必须在 0% 到 100% 之间。";
            }
            if (!between(scoreWidth, 220, 600) || !between(textWidth, 140, 500)) {
                return "HUD 宽度超出允许范围。";
            }
            if (!between(scoreScalePercent, 50, 160) || !between(textScalePercent, 50, 160)
                    || !between(feedScalePercent, 50, 160) || !between(bannerScalePercent, 50, 160)) {
                return "HUD 大小必须在 50% 到 160% 之间。";
            }
            if (!between(scoreOpacityPercent, 20, 100) || !between(textOpacityPercent, 20, 100)
                    || !between(feedOpacityPercent, 20, 100) || !between(bannerOpacityPercent, 20, 100)) {
                return "HUD 透明度必须在允许范围内。";
            }
            if (tooLong(scoreHeaderTemplate) || tooLong(scoreTeamATemplate) || tooLong(scoreTeamBTemplate)
                    || tooLong(scoreTimerTemplate) || tooLong(scoreDetailsTemplate) || tooLong(textTemplate)
                    || tooLong(feedTemplate) || tooLong(bannerLineOneTemplate) || tooLong(bannerLineTwoTemplate)) {
                return "HUD 模板不能超过 128 个字符。";
            }
            return null;
        }

        /** 用于快速恢复内置组件的文字模板与强调色。 */
        public Elements withTemplates(String scoreHeader, String scoreA, String scoreB, String timer,
                                      String details, String text, String feed, String bannerOne,
                                      String bannerTwo) {
            return new Elements(scoreVisible, scoreXPercent, scoreYPercent, scoreWidth, scoreScalePercent,
                    scoreOpacityPercent, textVisible, textXPercent, textYPercent, textWidth, textScalePercent,
                    textOpacityPercent, feedVisible, feedXPercent, feedYPercent, feedScalePercent,
                    feedOpacityPercent, bannerVisible, bannerXPercent, bannerYPercent, bannerScalePercent,
                    bannerOpacityPercent, safeTemplate(scoreHeader, scoreHeaderTemplate),
                    safeTemplate(scoreA, scoreTeamATemplate), safeTemplate(scoreB, scoreTeamBTemplate),
                    safeTemplate(timer, scoreTimerTemplate), safeTemplate(details, scoreDetailsTemplate),
                    safeTemplate(text, textTemplate), safeTemplate(feed, feedTemplate),
                    safeTemplate(bannerOne, bannerLineOneTemplate), safeTemplate(bannerTwo, bannerLineTwoTemplate),
                    scoreColor, textColor, feedColor, bannerColor, builtInMask);
        }

        public boolean builtInEnabled(HudContext.BuiltIn component) {
            return component != null && (builtInMask & builtInBit(component)) != 0;
        }

        private static String safeTemplate(String value, String fallback) {
            return value == null ? fallback : value;
        }

        private static boolean tooLong(String value) {
            return value != null && value.length() > 128;
        }

        public Mutable mutable() {
            return new Mutable(this);
        }

        JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("scoreVisible", scoreVisible);
            object.addProperty("scoreXPercent", scoreXPercent);
            object.addProperty("scoreYPercent", scoreYPercent);
            object.addProperty("scoreWidth", scoreWidth);
            object.addProperty("scoreScalePercent", scoreScalePercent);
            object.addProperty("scoreOpacityPercent", scoreOpacityPercent);
            object.addProperty("textVisible", textVisible);
            object.addProperty("textXPercent", textXPercent);
            object.addProperty("textYPercent", textYPercent);
            object.addProperty("textWidth", textWidth);
            object.addProperty("textScalePercent", textScalePercent);
            object.addProperty("textOpacityPercent", textOpacityPercent);
            object.addProperty("feedVisible", feedVisible);
            object.addProperty("feedXPercent", feedXPercent);
            object.addProperty("feedYPercent", feedYPercent);
            object.addProperty("feedScalePercent", feedScalePercent);
            object.addProperty("feedOpacityPercent", feedOpacityPercent);
            object.addProperty("bannerVisible", bannerVisible);
            object.addProperty("bannerXPercent", bannerXPercent);
            object.addProperty("bannerYPercent", bannerYPercent);
            object.addProperty("bannerScalePercent", bannerScalePercent);
            object.addProperty("bannerOpacityPercent", bannerOpacityPercent);
            object.addProperty("scoreHeaderTemplate", scoreHeaderTemplate);
            object.addProperty("scoreTeamATemplate", scoreTeamATemplate);
            object.addProperty("scoreTeamBTemplate", scoreTeamBTemplate);
            object.addProperty("scoreTimerTemplate", scoreTimerTemplate);
            object.addProperty("scoreDetailsTemplate", scoreDetailsTemplate);
            object.addProperty("textTemplate", textTemplate);
            object.addProperty("feedTemplate", feedTemplate);
            object.addProperty("bannerLineOneTemplate", bannerLineOneTemplate);
            object.addProperty("bannerLineTwoTemplate", bannerLineTwoTemplate);
            object.addProperty("scoreColor", scoreColor);
            object.addProperty("textColor", textColor);
            object.addProperty("feedColor", feedColor);
            object.addProperty("bannerColor", bannerColor);
             object.addProperty("builtInMask", builtInMask);
            return object;
        }

        static Elements read(JsonObject object, Elements fallback) {
            return new Elements(
                    bool(object, "scoreVisible", fallback.scoreVisible),
                    integer(object, "scoreXPercent", fallback.scoreXPercent, 0, 100),
                    integer(object, "scoreYPercent", fallback.scoreYPercent, 0, 100),
                    integer(object, "scoreWidth", fallback.scoreWidth, 220, 600),
                    integer(object, "scoreScalePercent", fallback.scoreScalePercent, 50, 160),
                    integer(object, "scoreOpacityPercent", fallback.scoreOpacityPercent, 20, 100),
                    bool(object, "textVisible", fallback.textVisible),
                    integer(object, "textXPercent", fallback.textXPercent, 0, 100),
                    integer(object, "textYPercent", fallback.textYPercent, 0, 100),
                    integer(object, "textWidth", fallback.textWidth, 140, 500),
                    integer(object, "textScalePercent", fallback.textScalePercent, 50, 160),
                    integer(object, "textOpacityPercent", fallback.textOpacityPercent, 20, 100),
                    bool(object, "feedVisible", fallback.feedVisible),
                    integer(object, "feedXPercent", fallback.feedXPercent, 0, 100),
                    integer(object, "feedYPercent", fallback.feedYPercent, 0, 100),
                    integer(object, "feedScalePercent", fallback.feedScalePercent, 50, 160),
                    integer(object, "feedOpacityPercent", fallback.feedOpacityPercent, 20, 100),
                    bool(object, "bannerVisible", fallback.bannerVisible),
                    integer(object, "bannerXPercent", fallback.bannerXPercent, 0, 100),
                    integer(object, "bannerYPercent", fallback.bannerYPercent, 0, 100),
                    integer(object, "bannerScalePercent", fallback.bannerScalePercent, 50, 160),
                    integer(object, "bannerOpacityPercent", fallback.bannerOpacityPercent, 20, 100),
                     string(object, "scoreHeaderTemplate", fallback.scoreHeaderTemplate),
                     string(object, "scoreTeamATemplate", fallback.scoreTeamATemplate),
                     string(object, "scoreTeamBTemplate", fallback.scoreTeamBTemplate),
                     string(object, "scoreTimerTemplate", fallback.scoreTimerTemplate),
                     string(object, "scoreDetailsTemplate", fallback.scoreDetailsTemplate),
                     string(object, "textTemplate", fallback.textTemplate),
                     string(object, "feedTemplate", fallback.feedTemplate),
                     string(object, "bannerLineOneTemplate", fallback.bannerLineOneTemplate),
                     string(object, "bannerLineTwoTemplate", fallback.bannerLineTwoTemplate),
                     integer(object, "scoreColor", fallback.scoreColor),
                     integer(object, "textColor", fallback.textColor),
                     integer(object, "feedColor", fallback.feedColor),
                     integer(object, "bannerColor", fallback.bannerColor),
                     integer(object, "builtInMask", fallback.builtInMask, 0, ALL_BUILT_IN_BITS));
        }

        private static boolean between(int value, int minimum, int maximum) {
            return value >= minimum && value <= maximum;
        }
    }

    /** 元素的草稿可变镜像。 */
    public static final class Mutable {
        public boolean scoreVisible;
        public int scoreXPercent;
        public int scoreYPercent;
        public int scoreWidth;
        public int scoreScalePercent;
        public int scoreOpacityPercent;
        public boolean textVisible;
        public int textXPercent;
        public int textYPercent;
        public int textWidth;
        public int textScalePercent;
        public int textOpacityPercent;
        public boolean feedVisible;
        public int feedXPercent;
        public int feedYPercent;
        public int feedScalePercent;
        public int feedOpacityPercent;
        public boolean bannerVisible;
        public int bannerXPercent;
        public int bannerYPercent;
        public int bannerScalePercent;
        public int bannerOpacityPercent;
        public String scoreHeaderTemplate;
        public String scoreTeamATemplate;
        public String scoreTeamBTemplate;
        public String scoreTimerTemplate;
        public String scoreDetailsTemplate;
        public String textTemplate;
        public String feedTemplate;
        public String bannerLineOneTemplate;
        public String bannerLineTwoTemplate;
        public int scoreColor;
        public int textColor;
        public int feedColor;
        public int bannerColor;
         public int builtInMask;

        Mutable(Elements source) {
            scoreVisible = source.scoreVisible;
            scoreXPercent = source.scoreXPercent;
            scoreYPercent = source.scoreYPercent;
            scoreWidth = source.scoreWidth;
            scoreScalePercent = source.scoreScalePercent;
            scoreOpacityPercent = source.scoreOpacityPercent;
            textVisible = source.textVisible;
            textXPercent = source.textXPercent;
            textYPercent = source.textYPercent;
            textWidth = source.textWidth;
            textScalePercent = source.textScalePercent;
            textOpacityPercent = source.textOpacityPercent;
            feedVisible = source.feedVisible;
            feedXPercent = source.feedXPercent;
            feedYPercent = source.feedYPercent;
            feedScalePercent = source.feedScalePercent;
            feedOpacityPercent = source.feedOpacityPercent;
            bannerVisible = source.bannerVisible;
            bannerXPercent = source.bannerXPercent;
            bannerYPercent = source.bannerYPercent;
            bannerScalePercent = source.bannerScalePercent;
            bannerOpacityPercent = source.bannerOpacityPercent;
            scoreHeaderTemplate = source.scoreHeaderTemplate;
            scoreTeamATemplate = source.scoreTeamATemplate;
            scoreTeamBTemplate = source.scoreTeamBTemplate;
            scoreTimerTemplate = source.scoreTimerTemplate;
            scoreDetailsTemplate = source.scoreDetailsTemplate;
            textTemplate = source.textTemplate;
            feedTemplate = source.feedTemplate;
            bannerLineOneTemplate = source.bannerLineOneTemplate;
            bannerLineTwoTemplate = source.bannerLineTwoTemplate;
            scoreColor = source.scoreColor;
            textColor = source.textColor;
            feedColor = source.feedColor;
            bannerColor = source.bannerColor;
             builtInMask = source.builtInMask;
         }

         public boolean builtInEnabled(HudContext.BuiltIn component) {
             return component != null && (builtInMask & builtInBit(component)) != 0;
         }

         public void setBuiltInEnabled(HudContext.BuiltIn component, boolean enabled) {
             if (component == null) {
                 return;
             }
             int bit = builtInBit(component);
             builtInMask = enabled ? builtInMask | bit : builtInMask & ~bit;
         }

        public Elements build() {
            return new Elements(scoreVisible, scoreXPercent, scoreYPercent, scoreWidth,
                    scoreScalePercent, scoreOpacityPercent, textVisible, textXPercent, textYPercent,
                    textWidth, textScalePercent, textOpacityPercent, feedVisible, feedXPercent,
                    feedYPercent, feedScalePercent, feedOpacityPercent, bannerVisible, bannerXPercent,
                    bannerYPercent, bannerScalePercent, bannerOpacityPercent, scoreHeaderTemplate,
                    scoreTeamATemplate, scoreTeamBTemplate, scoreTimerTemplate, scoreDetailsTemplate,
                    textTemplate, feedTemplate, bannerLineOneTemplate, bannerLineTwoTemplate, scoreColor,
                    textColor, feedColor, bannerColor, builtInMask);
        }
    }

    /** 跨场景共用的背景与参考线设置。 */
    public record Global(boolean guidesVisible, String backgroundFile, int backgroundOpacityPercent) {
        public static Global defaults() {
            return new Global(true, "", 60);
        }

        public String validationError() {
            if (backgroundOpacityPercent < 0 || backgroundOpacityPercent > 100) {
                return "背景透明度必须在 0% 到 100% 之间。";
            }
            return null;
        }

        public MutableGlobal mutable() {
            return new MutableGlobal(this);
        }

        JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("guidesVisible", guidesVisible);
            object.addProperty("backgroundFile", backgroundFile);
            object.addProperty("backgroundOpacityPercent", backgroundOpacityPercent);
            return object;
        }

        static Global read(JsonObject object) {
            return new Global(
                    bool(object, "guidesVisible", true),
                    string(object, "backgroundFile", ""),
                    integer(object, "backgroundOpacityPercent", 60, 0, 100));
        }
    }

    public static final class MutableGlobal {
        public boolean guidesVisible;
        public String backgroundFile;
        public int backgroundOpacityPercent;

        MutableGlobal(Global source) {
            guidesVisible = source.guidesVisible;
            backgroundFile = source.backgroundFile;
            backgroundOpacityPercent = source.backgroundOpacityPercent;
        }

        public Global build() {
            return new Global(guidesVisible, backgroundFile == null ? "" : backgroundFile,
                    backgroundOpacityPercent);
        }
    }

    /**
     * 一个真正独立的 HUD 模块，可用于文字、色块、进度条或图片。
     * 位置、尺寸、背景和透明度都独立保存；{@code source} 非空时文字 / 进度条
     * 的内容来自 {@link HudStats} 注册的统计接口（如房间总击杀进度、我的伤害等），
     * {@code scalePercent} 控制文字模块的字号（50%～300%）。
     */
    public record CustomElement(String id, String type, String text, int xPercent, int yPercent,
                                int width, int height, int opacityPercent, int color, boolean visible,
                                String source, int scalePercent, boolean background, boolean border, boolean shadow) {
        public CustomElement {
            id = id == null || id.isBlank() ? "element" : id;
            type = type == null ? "text" : type;
            text = text == null ? "自定义元素" : text;
            source = source == null ? "" : source;
            scalePercent = Math.max(50, Math.min(300, scalePercent));
        }

        /** 旧十参构造：未绑定数据源、默认字号。 */
        public CustomElement(String id, String type, String text, int xPercent, int yPercent,
                             int width, int height, int opacityPercent, int color, boolean visible) {
            this(id, type, text, xPercent, yPercent, width, height, opacityPercent, color, visible,
                    "", 100, false, false, false);
        }

        /** 旧十二参构造：保留原有数据源与字号参数，不附带装饰。 */
        public CustomElement(String id, String type, String text, int xPercent, int yPercent,
                             int width, int height, int opacityPercent, int color, boolean visible,
                             String source, int scalePercent) {
            this(id, type, text, xPercent, yPercent, width, height, opacityPercent, color, visible,
                    source, scalePercent, false, false, false);
        }

        public static CustomElement text(String id) {
            return new CustomElement(id, "text", "自定义文字", 50, 50, 180, 24, 85,
                    UiTheme.TEXT, true, "", 100, false, false, false);
        }

        public static CustomElement block(String id) {
            return new CustomElement(id, "block", "", 50, 50, 180, 40, 70,
                    UiTheme.ACCENT, true, "", 100, false, false, false);
        }

        public static CustomElement progress(String id) {
            return new CustomElement(id, "progress", "进度", 50, 60, 220, 16, 85,
                    UiTheme.INFO, true, "", 100, false, false, false);
        }

        public static CustomElement image(String id, String fileName) {
            return new CustomElement(id, "image", fileName, 50, 50, 240, 120, 85,
                    UiTheme.ACCENT, true, "", 100, false, false, false);
        }

        /** 该模块绑定的统计接口（未绑定返回 null）。 */
        public HudStats.Source boundSource() {
            return source.isBlank() ? null : HudStats.byId(source);
        }

        public boolean bound() {
            return !source.isBlank() && boundSource() != null;
        }

        public String displayName() {
            String boundLabel = bound() ? " → " + boundSource().name() : "";
            return switch (type) {
                case "block" -> "色块 · " + id;
                case "progress" -> "进度条 · " + id + boundLabel;
                case "image" -> "图片 · " + (text.isBlank() ? "未选择" : text);
                default -> "文字 · " + id + boundLabel;
            };
        }

        public String validationError() {
            if (xPercent < 0 || xPercent > 100 || yPercent < 0 || yPercent > 100) {
                return "自定义 HUD 位置必须在 0% 到 100% 之间。";
            }
            if (width < 8 || width > 1000 || height < 8 || height > 400) {
                return "自定义 HUD 尺寸超出允许范围。";
            }
            if (opacityPercent < 0 || opacityPercent > 100) {
                return "自定义 HUD 透明度必须在 0% 到 100% 之间。";
            }
            if (scalePercent < 50 || scalePercent > 300) {
                return "文字大小必须在 50% 到 300% 之间。";
            }
            return null;
        }

        JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("id", id); object.addProperty("type", type); object.addProperty("text", text);
            object.addProperty("xPercent", xPercent); object.addProperty("yPercent", yPercent);
            object.addProperty("width", width); object.addProperty("height", height);
            object.addProperty("opacityPercent", opacityPercent); object.addProperty("color", color);
            object.addProperty("visible", visible);
            object.addProperty("source", source); object.addProperty("scalePercent", scalePercent);
            object.addProperty("background", background);
            object.addProperty("border", border);
            object.addProperty("shadow", shadow);
            return object;
        }

        static CustomElement read(JsonObject object) {
            return new CustomElement(string(object, "id", "element"), string(object, "type", "text"),
                    string(object, "text", "自定义元素"), integer(object, "xPercent", 50, 0, 100),
                    integer(object, "yPercent", 50, 0, 100), integer(object, "width", 180, 8, 1000),
                    integer(object, "height", 24, 8, 400), integer(object, "opacityPercent", 85, 0, 100),
                    integer(object, "color", UiTheme.ACCENT), bool(object, "visible", true),
                    string(object, "source", ""), integer(object, "scalePercent", 100, 50, 300),
                     bool(object, "background", false), bool(object, "border", false),
                     bool(object, "shadow", false));
        }
    }

    /** 全量配置快照：五个场景 + 全局 + 自定义模块。 */
    public record Snapshot(Global global, Map<HudContext, Elements> contexts,
                           Map<HudContext, List<CustomElement>> customElements) {
        public Snapshot {
            global = global == null ? Global.defaults() : global;
            EnumMap<HudContext, Elements> copy = new EnumMap<>(HudContext.class);
            EnumMap<HudContext, List<CustomElement>> customCopy = new EnumMap<>(HudContext.class);
            for (HudContext context : HudContext.values()) {
                Elements element = contexts == null ? null : contexts.get(context);
                copy.put(context, element == null ? Elements.defaultsFor(context) : element);
                List<CustomElement> modules = customElements == null ? null : customElements.get(context);
                customCopy.put(context, modules == null ? List.of() : List.copyOf(modules));
            }
            contexts = copy;
            customElements = customCopy;
        }

        public Snapshot(Global global, Map<HudContext, Elements> contexts) {
            this(global, contexts, Map.of());
        }

        public Elements elements(HudContext context) {
            return contexts.getOrDefault(context, Elements.defaultsFor(context));
        }

        public List<CustomElement> customElements(HudContext context) {
            return customElements.getOrDefault(context, List.of());
        }

        public String validationError() {
            String error = global.validationError();
            if (error != null) return error;
            for (Elements element : contexts.values()) {
                error = element.validationError();
                if (error != null) return error;
            }
            for (List<CustomElement> modules : customElements.values()) {
                for (CustomElement module : modules) {
                    error = module.validationError();
                    if (error != null) return error;
                }
            }
            return null;
        }

        public Draft draft() { return new Draft(this); }
    }

    /** 配置窗编辑用的可草稿快照。 */
    public static final class Draft {
        public final MutableGlobal global;
        private final EnumMap<HudContext, Mutable> contexts = new EnumMap<>(HudContext.class);
        private final EnumMap<HudContext, List<CustomElement>> customElements = new EnumMap<>(HudContext.class);

        Draft(Snapshot source) {
            global = source.global().mutable();
            for (HudContext context : HudContext.values()) {
                contexts.put(context, source.elements(context).mutable());
                customElements.put(context, new ArrayList<>(source.customElements(context)));
            }
        }

        public Mutable of(HudContext context) {
            return contexts.getOrDefault(context, Elements.defaultsFor(context).mutable());
        }

        public Elements elements(HudContext context) { return of(context).build(); }
        public List<CustomElement> customElements(HudContext context) {
            return Collections.unmodifiableList(customElements.getOrDefault(context, List.of()));
        }
        public void addCustomElement(HudContext context, CustomElement element) {
            customElements.computeIfAbsent(context, ignored -> new ArrayList<>()).add(element);
        }
        public void removeCustomElement(HudContext context, String id) {
            List<CustomElement> list = customElements.get(context);
            if (list != null) list.removeIf(element -> element.id().equals(id));
        }
        public void replaceCustomElement(HudContext context, CustomElement replacement) {
            List<CustomElement> list = customElements.computeIfAbsent(context, ignored -> new ArrayList<>());
            for (int index = 0; index < list.size(); index++) {
                if (list.get(index).id().equals(replacement.id())) { list.set(index, replacement); return; }
            }
        }
        public Snapshot build() {
            EnumMap<HudContext, Elements> built = new EnumMap<>(HudContext.class);
            for (Map.Entry<HudContext, Mutable> entry : contexts.entrySet()) built.put(entry.getKey(), entry.getValue().build());
            EnumMap<HudContext, List<CustomElement>> modules = new EnumMap<>(HudContext.class);
            for (HudContext context : HudContext.values()) modules.put(context, List.copyOf(customElements(context)));
            return new Snapshot(global.build(), built, modules);
        }
    }
}
