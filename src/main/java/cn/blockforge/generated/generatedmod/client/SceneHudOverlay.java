package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.screen.HudLayoutScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingManager;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingStatus;
import cn.blockforge.generated.generatedmod.lobby.RoomState;
import cn.blockforge.generated.generatedmod.lobby.RoomView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 大厅场景 HUD：比赛之外，在“游戏主界面”常驻显示两类横幅——
 * 正在匹配（排队 / 成局准备）与房间中（所在房间的实时概况）。
 * 两者都能在 HUD 配置窗的“正在匹配”“房间中”场景里单独调整位置、大小与透明度。
 */
public final class SceneHudOverlay {
    private SceneHudOverlay() {
    }

    public static void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                              int width, int height) {
        // 模组配置界面已经有自己的面板层，实时场景 HUD 不应盖到窗口内容和控件上。
        if (Minecraft.getInstance().screen instanceof UiScreen
                || Minecraft.getInstance().screen instanceof HudLayoutScreen
                || ClientMatchData.inMatch() || width <= 0 || height <= 0) {
            return;
        }
        ClientHudLayout.Global global = ClientHudLayout.global();
        MatchmakingStatus status = ClientLobbyData.matchmaking();
        RoomView room = ownRoom();
        boolean forming = ClientLobbyData.dynamicReadySeconds() > 0;
        if (!global.backgroundFile().isBlank()) {
            HudBackground.drawStretch(graphics, global.backgroundFile(),
                    global.backgroundOpacityPercent(), width, height);
        }
        if (!status.queued() && !forming) {
            if (room == null) {
                renderGlobalCustom(graphics, width, height);
                return;
            }
            renderGlobalCustom(graphics, width, height);
            renderRoom(graphics, partialTick, width, height, room);
            return;
        }
        renderGlobalCustom(graphics, width, height);
        renderMatching(graphics, partialTick, width, height, status, forming, room);
    }

    private static void renderMatching(GuiGraphics graphics, float partialTick, int width, int height,
                                       MatchmakingStatus status, boolean forming, RoomView room) {
        ClientHudLayout.Elements elements = ClientHudLayout.elements(HudContext.MATCHING);
        Font font = Minecraft.getInstance().font;
        if (elements.builtInEnabled(HudContext.BuiltIn.BANNER) && elements.bannerVisible() && !renderOverride(graphics, font, HudContext.MATCHING,
                elements, width, height, partialTick)) {
            Map<String, String> values = matchingTemplateValues(status, forming, room);
            drawBanner(graphics, font, elements, width, height,
                    HudStats.resolveTemplate(elements.bannerLineOneTemplate(), values),
                    HudStats.resolveTemplate(elements.bannerLineTwoTemplate(), values), elements.bannerColor());
        }
        renderCustom(graphics, font, width, height, HudContext.MATCHING);
    }

    /** 匹配横幅第一行；公开给 HUD 统计接口作为「正在匹配」场景的文字数据源。 */
    public static String matchingLineOne(MatchmakingStatus status, boolean forming, RoomView room) {
        if (forming) {
            return "已匹配！" + ClientLobbyData.dynamicReadySeconds() + " 秒后开赛";
        }
        return "正在匹配  " + status.queueSize() + "/" + MatchmakingManager.MIN_PLAYERS_TO_FORM + " 人成局";
    }

    /** 匹配横幅第二行；公开给 HUD 统计接口。 */
    public static String matchingLineTwo(MatchmakingStatus status, boolean forming, RoomView room) {
        if (forming) {
            return room == null ? "准备进入比赛，请稍候"
                    : room.name() + "  ·  人数 " + room.memberCount();
        }
        return "已等待 " + UiTheme.formatTicks(ClientLobbyData.dynamicWaitedTicks())
                + "  ·  序位 " + Math.max(1, status.position());
    }

    private static void renderRoom(GuiGraphics graphics, float partialTick, int width, int height, RoomView room) {
        ClientHudLayout.Elements elements = ClientHudLayout.elements(HudContext.ROOM);
        Font font = Minecraft.getInstance().font;
        if (elements.builtInEnabled(HudContext.BuiltIn.BANNER) && elements.bannerVisible()) {
            if (!renderOverride(graphics, font, HudContext.ROOM, elements, width, height, partialTick)) {
                Map<String, String> values = roomTemplateValues(room);
                drawBanner(graphics, font, elements, width, height,
                        HudStats.resolveTemplate(elements.bannerLineOneTemplate(), values),
                        HudStats.resolveTemplate(elements.bannerLineTwoTemplate(), values), elements.bannerColor());
            }
        }
        renderCustom(graphics, font, width, height, HudContext.ROOM);
    }

    private static boolean renderOverride(GuiGraphics graphics, Font font, HudContext context,
                                          ClientHudLayout.Elements elements, int width, int height,
                                          float partialTick) {
        HudApi.BuiltInRenderer renderer = HudApi.builtInRenderer(context, HudContext.BuiltIn.BANNER);
        if (renderer == null) {
            return false;
        }
        try {
            return renderer.render(graphics, font, context, HudContext.BuiltIn.BANNER,
                    elements, width, height, partialTick);
        } catch (RuntimeException error) {
            return false;
        }
    }

    /** 房间横幅第一行；公开给 HUD 统计接口作为「房间中」场景的文字数据源。 */
    public static String roomLineOne(RoomView room) {
        return "房间  " + (room.name().isBlank() ? room.id() : room.name())
                + (room.locked() ? "  ·  私有" : "")
                + "  ·  " + room.rules().mode().displayName()
                + "  ·  地图 " + ClientLobbyData.mapDisplayName(room.mapId());
    }

    /** 房间横幅第二行；公开给 HUD 统计接口。 */
    public static String roomLineTwo(RoomView room) {
        return "人数 " + room.memberCount()
                + (room.maxPlayers() <= 0 ? "/不限" : "/" + room.maxPlayers())
                + "  ·  房主 " + (room.owner() == null || room.owner().isBlank() ? "—" : room.owner())
                + "  ·  " + stateLabel(room.state())
                + (ClientLobbyData.ownOwner() ? "  ·  你是房主" : "");
    }

    private static void renderCustom(GuiGraphics graphics, Font font, int width, int height, HudContext context) {
        HudCustomRenderer.render(graphics, font, ClientHudLayout.customElements(context),
                width, height, false, false, "", context);
    }

    private static void renderGlobalCustom(GuiGraphics graphics, int width, int height) {
        HudCustomRenderer.render(graphics, Minecraft.getInstance().font,
                ClientHudLayout.customElements(HudContext.GLOBAL), width, height, false, false,
                "", HudContext.GLOBAL);
    }

    private static Map<String, String> matchingTemplateValues(MatchmakingStatus status,
                                                               boolean forming, RoomView room) {
        Map<String, String> values = new LinkedHashMap<>(
                HudParameters.values(false, HudContext.MATCHING, true));
        values.put("matching_line1", matchingLineOne(status, forming, room));
        values.put("matching_line2", matchingLineTwo(status, forming, room));
        values.put("queue", Integer.toString(status.queueSize()));
        values.put("position", Integer.toString(Math.max(1, status.position())));
        values.put("wait", UiTheme.formatTicks(ClientLobbyData.dynamicWaitedTicks()));
        values.put("ready", Integer.toString(ClientLobbyData.dynamicReadySeconds()));
        return values;
    }

    private static Map<String, String> roomTemplateValues(RoomView room) {
        Map<String, String> values = new LinkedHashMap<>(
                HudParameters.values(false, HudContext.ROOM, true));
        values.put("room_line1", roomLineOne(room));
        values.put("room_line2", roomLineTwo(room));
        values.put("room", room.name().isBlank() ? room.id() : room.name());
        values.put("map", ClientLobbyData.mapDisplayName(room.mapId()));
        values.put("members", Integer.toString(room.memberCount()));
        values.put("owner", room.owner() == null ? "" : room.owner());
        values.put("state", stateLabel(room.state()));
        return values;
    }

    private static void drawBanner(GuiGraphics graphics, Font font,
                                   ClientHudLayout.Elements elements, int width, int height,
                                   String lineOne, String lineTwo, int accent) {
        int baseWidth = 340;
        HudGeometry.Rect panel = HudGeometry.banner(elements, width, height, baseWidth);
        MatchHudOverlay.drawPanel(graphics, font, panel, HudGeometry.BANNER_BASE_HEIGHT,
                UiTheme.fit(font, lineOne, panel.baseWidth() - 16),
                UiTheme.fit(font, lineTwo, panel.baseWidth() - 16), accent,
                elements.bannerOpacityPercent());
    }

    private static String stateLabel(RoomState state) {
        return switch (state) {
            case OPEN -> "开放中";
            case COUNTDOWN -> "开赛倒计时";
            case WAITING_MAP -> "等待地图";
            case RUNNING -> "比赛进行中";
        };
    }

    /** 自己所在的房间视图；公开给 HUD 统计接口（未在任何房间返回 null）。 */
    public static RoomView ownRoom() {
        String ownId = ClientLobbyData.ownRoomId();
        if (ownId.isBlank()) {
            return null;
        }
        for (RoomView room : ClientLobbyData.rooms()) {
            if (room.id().equals(ownId)) {
                return room;
            }
        }
        return null;
    }
}
