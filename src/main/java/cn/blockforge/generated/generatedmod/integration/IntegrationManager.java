package cn.blockforge.generated.generatedmod.integration;

import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class IntegrationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_integration");
    private static final List<String> GD656_IDS = List.of(
            "gd656killicon", "gd656_killicon", "gd656killcon", "gd656_killcon", "killcon");
    private static boolean detected;
    private static boolean taczLoaded;
    private static boolean gd656Loaded;

    private IntegrationManager() {
    }

    public static void detect() {
        if (detected) {
            return;
        }
        detected = true;
        try {
            ModList mods = ModList.get();
            taczLoaded = mods.isLoaded("tacz");
            gd656Loaded = GD656_IDS.stream().anyMatch(mods::isLoaded);
        } catch (Throwable error) {
            LOGGER.warn("读取可选 Mod 状态失败，将使用 Forge 原生事件兜底", error);
        }
        if (taczLoaded) {
            LOGGER.info("已检测到 TACZ，枪械、弹药和伤害继续由 TACZ 负责");
        } else {
            LOGGER.info("未检测到 TACZ，模组不会创建替代枪械系统");
        }
        if (gd656Loaded) {
            LOGGER.info("已检测到 GD656Killicon；未绑定未经确认的私有计分接口，比赛进度使用 Forge 死亡事件作为兼容兜底");
        } else {
            LOGGER.info("未检测到 GD656Killicon，比赛进度使用 Forge 原生死亡事件");
        }
    }

    public static boolean isTaczLoaded() {
        return taczLoaded;
    }

    public static boolean isGd656Loaded() {
        return gd656Loaded;
    }

    public static String statusText() {
        String tacz = taczLoaded ? "TACZ=已检测" : "TACZ=未检测";
        String gd = gd656Loaded ? "GD656Killicon=已检测，Forge事件兜底" : "GD656Killicon=未检测，Forge事件";
        return tacz + "，" + gd;
    }
}
