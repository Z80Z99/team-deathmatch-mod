package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.item.ModItems;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.ArrayList;
import java.util.List;

/** 手持地图道具时的左下操作提示与右下目标信息。 */
public final class MapToolHud {
    private static final int PANEL_WIDTH = 270;
    private static final int LINE_HEIGHT = 10;

    private MapToolHud() {
    }

    public static void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                              int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || screenWidth <= 0 || screenHeight <= 0) {
            return;
        }
        boolean planner = minecraft.player.getMainHandItem().is(ModItems.MAP_PLANNER.get())
                || minecraft.player.getOffhandItem().is(ModItems.MAP_PLANNER.get());
        boolean brush = minecraft.player.getMainHandItem().is(ModItems.MAP_BRUSH.get())
                || minecraft.player.getOffhandItem().is(ModItems.MAP_BRUSH.get());
        if (!planner && !brush) return;

        MapEditorView view = ClientMapEditorData.view();
        Font font = minecraft.font;
        List<String> left = new ArrayList<>();
        if (planner) {
            left.add("规划器：右键打开菜单");
            left.add("左键选定准星区域");
            left.add("悬停区域会呼吸发光");
            left.add("菜单内可编辑区域属性");
        }
        if (brush) {
            left.add(view.selectedRegionId().isBlank() ? "画笔未启用：先用规划器选择目标"
                    : "编辑：" + view.regions().stream().filter(region -> region.id().equals(view.selectedRegionId()))
                    .map(MapRegion::displayName).findFirst().orElse(view.selectedTool().displayName()));
            boolean point = view.selectedTool().kind() == cn.blockforge.generated.generatedmod.map.MapTool.Kind.POINT;
            boolean corners = view.brushMode() == cn.blockforge.generated.generatedmod.map.MapBrushMode.REGION;
            left.add(point ? "左键：移除出生点" : corners ? "左键：选择第一个角（实际方块）" : "左键：收缩区域边缘");
            left.add(point ? "右键：添加出生点" : corners ? "右键：选择对角，自动保存" : "右键：扩展区域到目标位置");
            left.add("蹲+右键：画笔菜单");
            left.add("右键距离：" + view.brushRange() + "格");
            if (ClientMapEditorData.firstPoint() != null) left.add("第一角：" + ClientMapEditorData.firstPoint().toShortString());
            if (ClientMapEditorData.secondPoint() != null) left.add("第二角：" + ClientMapEditorData.secondPoint().toShortString());
            if (!view.message().isBlank()) left.add(view.message());
        }
        drawPanel(graphics, font, left, 8, screenHeight - 8 - left.size() * LINE_HEIGHT - 8,
                PANEL_WIDTH, UiTheme.INFO);

        List<String> right = new ArrayList<>();
        MapRegion hovered = region(view, MapToolClientState.hoveredRegionId());
        if (hovered != null) {
            right.add("区域：" + hovered.displayName());
            right.add("类型：" + hovered.type().displayName());
            right.add("范围：" + hovered.region());
        }
        if (brush) {
            BlockPos target = MapToolClientState.brushTarget();
            right.add("目标：" + target.getX() + ", " + target.getY() + ", " + target.getZ());
            right.add("状态：" + (MapToolClientState.brushTargetAir()
                    ? "空气（右键可选）" : "方块（左右键可用）"));
        }
        if (!right.isEmpty()) {
            int width = Math.max(170, right.stream().mapToInt(font::width).max().orElse(160) + 24);
            drawPanel(graphics, font, right, screenWidth - width - 8,
                    screenHeight - 8 - right.size() * LINE_HEIGHT - 8, width, UiTheme.ACCENT);
        }
    }

    private static MapRegion region(MapEditorView view, String id) {
        for (MapRegion region : view.regions()) if (region.id().equals(id)) return region;
        return null;
    }

    private static void drawPanel(GuiGraphics graphics, Font font, List<String> lines,
                                  int x, int y, int width, int accent) {
        int height = lines.size() * LINE_HEIGHT + 10;
        graphics.fill(x, y, x + width, y + height, 0xB0101113);
        graphics.fill(x, y, x + 2, y + height, accent);
        graphics.renderOutline(x, y, width, height, UiTheme.BORDER_SUBTLE);
        int textY = y + 6;
        for (String line : lines) {
            graphics.drawString(font, UiTheme.fit(font, line, width - 12), x + 9, textY,
                    UiTheme.TEXT, false);
            textY += LINE_HEIGHT;
        }
    }
}
