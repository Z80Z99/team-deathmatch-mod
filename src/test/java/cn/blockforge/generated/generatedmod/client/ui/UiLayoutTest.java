package cn.blockforge.generated.generatedmod.client.ui;

import cn.blockforge.generated.generatedmod.client.screen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UiLayoutTest {
    @Test void rebuildingScrollingContentKeepsCurrentScrollPosition() {
        ScrollTestScreen screen = new ScrollTestScreen();
        screen.width = 320;
        screen.height = 240;
        screen.rebuildWidgets();

        screen.mouseScrolled(160, 120, -5);
        int before = screen.scrollPosition();
        assertTrue(before > 0);

        screen.rebuildWidgets();
        assertEquals(before, screen.scrollPosition());
    }

    @Test void builtInPresetsAreValidAndRepeatableForEveryScene() {
        for (var context : cn.blockforge.generated.generatedmod.client.HudContext.values()) {
            for (var preset : cn.blockforge.generated.generatedmod.client.HudPreset.values()) {
                var values = cn.blockforge.generated.generatedmod.client.ClientHudLayout.Elements.defaultsFor(context).mutable();
                preset.apply(values, context);
                var first = values.build();
                assertNull(first.validationError(), preset.label() + context);
                assertEquals(context.isMatch(), first.scoreVisible());
                assertEquals(!context.isMatch(), first.bannerVisible());
                assertEquals(context == cn.blockforge.generated.generatedmod.client.HudContext.ROOM
                        ? "{room_line1}" : "{matching_line1}", first.bannerLineOneTemplate());
                preset.apply(values, context);
                assertEquals(first, values.build());
            }
        }
    }

    @Test void templateFieldStartsAtBeginningAfterPlaceholderWidth() throws Exception {
        Font font = mock(Font.class);
        when(font.plainSubstrByWidth(anyString(), anyInt())).thenAnswer(call ->
                UiRenderCapture.shorten(call.getArgument(0), call.getArgument(1), false));
        when(font.plainSubstrByWidth(anyString(), anyInt(), anyBoolean())).thenAnswer(call ->
                UiRenderCapture.shorten(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
        UiEditBox editor = new UiEditBox(font, 0, 0, 10, 20,
                net.minecraft.network.chat.Component.literal("Template"));
        editor.setValue("{matching_line1}");
        editor.setWidth(220);
        assertEquals(0, editor.getCursorPosition());
        Field display = net.minecraft.client.gui.components.EditBox.class.getDeclaredField("displayPos");
        display.setAccessible(true);
        assertEquals(0, display.getInt(editor));
        assertEquals(212, editor.getInnerWidth());
        assertEquals("{matching_line1}", editor.getValue());
    }

    @org.junit.jupiter.api.AfterEach void clearClientData() {
        cn.blockforge.generated.generatedmod.client.ClientLobbyData.clear();
        cn.blockforge.generated.generatedmod.client.ClientMapEditorData.clear();
    }
    @Test void screensKeepVisibleControlsInBoundsAndSeparate() throws Exception {
        Minecraft minecraft = mock(Minecraft.class);
        Font font = mock(Font.class);
        set(Minecraft.class, minecraft, "font", font);
        var options = mock(net.minecraft.client.Options.class);
        when(options.getCameraType()).thenReturn(net.minecraft.client.CameraType.FIRST_PERSON);
        set(Minecraft.class, minecraft, "options", options);
        set(Minecraft.class, minecraft, "gameDirectory", new java.io.File("build/ui-fixture"));
        when(font.width(anyString())).thenAnswer(call -> UiRenderCapture.measure(call.getArgument(0)));
        when(font.plainSubstrByWidth(anyString(), anyInt())).thenAnswer(call -> {
            String text = call.getArgument(0); int max = call.getArgument(1);
            return UiRenderCapture.shorten(text, max, false);
        });
        when(font.plainSubstrByWidth(anyString(), anyInt(), anyBoolean())).thenAnswer(call -> {
            String text = call.getArgument(0); int max = call.getArgument(1);
            return UiRenderCapture.shorten(text, max, call.getArgument(2));
        });
        try (MockedStatic<Minecraft> access = mockStatic(Minecraft.class)) {
            access.when(Minecraft::getInstance).thenReturn(minecraft);
            seedData();
            List<Supplier<Screen>> factories = List.of(() -> new LobbyMenuScreen(null), () -> new LobbyScreen(null),
                    () -> new RoomCreateScreen(null), () -> new RoomScreen(null), () -> new MatchmakingScreen(null),
                    () -> new MapLibraryScreen(null), () -> new RoomMapSelectScreen(null),
                    () -> new MapCreateScreen(null), () -> new MapBrushScreen(),
                    () -> new MapPlannerScreen(List.of()),
                    () -> new MapToolsTutorialScreen(null),
                    () -> rulesScreen(), () -> new HudLayoutScreen(null), () -> new HudParameterHelpScreen(null));
            for (int[] size : List.of(new int[]{320, 240}, new int[]{480, 270}, new int[]{640, 360}, new int[]{960, 540})) {
                for (Supplier<Screen> factory : factories) {
                    Screen screen = spy(factory.get());
                    doNothing().when(screen).renderBackground(any());
                    minecraft.screen = screen;
                    set(Screen.class, screen, "minecraft", minecraft); set(Screen.class, screen, "font", font);
                    screen.width = size[0]; screen.height = size[1];
                    Method init = screen.getClass().getDeclaredMethod("init"); init.setAccessible(true); init.invoke(screen);
                    List<AbstractWidget> widgets = screen.children().stream().filter(AbstractWidget.class::isInstance)
                            .map(AbstractWidget.class::cast).filter(widget -> widget.visible).toList();
                    for (AbstractWidget widget : widgets) {
                        String label = screen.getClass().getSimpleName() + " " + size[0] + "x" + size[1] + " " + widget.getMessage().getString();
                        assertTrue(widget.getX() >= 0 && widget.getY() >= 0 && widget.getX() + widget.getWidth() <= size[0]
                                && widget.getY() + widget.getHeight() <= size[1], label);
                        for (AbstractWidget other : widgets) if (widget != other) {
                            assertFalse(widget.getX() < other.getX() + other.getWidth() && widget.getX() + widget.getWidth() > other.getX()
                                    && widget.getY() < other.getY() + other.getHeight() && widget.getY() + widget.getHeight() > other.getY(), label);
                        }
                    }
                    try (UiRenderCapture capture = new UiRenderCapture(size[0], size[1])) {
                        screen.render(capture.graphics, -1000, -1000, 0);
                        if (screen instanceof HudLayoutScreen) {
                            assertTrue(capture.opaquePanelDepth >= 100, "Editor must cover preview glyph depth");
                            assertEquals(0f, capture.graphics.pose().last().pose().m32());
                        }
                        capture.save(screen.getClass().getSimpleName() + "-" + size[0] + "x" + size[1]);
                    }
                    if (screen instanceof RoomRulesScreen) {
                        screen.mouseScrolled(size[0] / 2.0, size[1] / 2.0, -12);
                        pressPrefix(screen, "目前不可用设置");
                        snapshot(screen, "不可用设置", size);
                        UiCycleButton<?> mode = (UiCycleButton<?>) screen.children().stream()
                                .filter(UiCycleButton.class::isInstance).findFirst().orElseThrow();
                        mode.onPress();
                        assertTrue(((UiChoiceHost) screen).choicePopup().isOpen());
                        snapshot(screen, "模式菜单", size);
                        screen.keyPressed(256, 0, 0);
                        assertFalse(((UiChoiceHost) screen).choicePopup().isOpen());
                    }
                    if (screen instanceof RoomScreen) {
                        Field listY = RoomScreen.class.getDeclaredField("listY"); listY.setAccessible(true);
                        Method fits = UiScreen.class.getDeclaredMethod("bandFits", int.class, int.class); fits.setAccessible(true);
                        assertEquals(true, fits.invoke(screen, listY.getInt(screen), 25), "First member row must be visible");
                        if (size[0] < 640) {
                            press(screen, ">");
                            snapshot(screen, "更多队伍", size);
                        }
                    }
                    if (screen instanceof HudLayoutScreen) {
                        press(screen, "属性"); snapshot(screen, "属性", size);
                        Field editDraft = HudLayoutScreen.class.getDeclaredField("draft"); editDraft.setAccessible(true);
                        var beforeDelete = ((cn.blockforge.generated.generatedmod.client.ClientHudLayout.Draft) editDraft.get(screen)).build();
                        press(screen, "删除此元素");
                        var afterDelete = ((cn.blockforge.generated.generatedmod.client.ClientHudLayout.Draft) editDraft.get(screen)).build();
                        assertNotEquals(beforeDelete, afterDelete);
                        Method undo = HudLayoutScreen.class.getDeclaredMethod("undo"); undo.setAccessible(true); undo.invoke(screen);
                        assertEquals(beforeDelete, ((cn.blockforge.generated.generatedmod.client.ClientHudLayout.Draft) editDraft.get(screen)).build());
                        press(screen, "死斗"); press(screen, ">"); snapshot(screen, "战斗预览", size);
                        Field draftField = HudLayoutScreen.class.getDeclaredField("draft");
                        draftField.setAccessible(true);
                        var draft = (cn.blockforge.generated.generatedmod.client.ClientHudLayout.Draft) draftField.get(screen);
                        for (var preset : cn.blockforge.generated.generatedmod.client.HudPreset.values()) {
                            preset.apply(draft,
                                    cn.blockforge.generated.generatedmod.client.HudContext.TEAM_DEATHMATCH);
                            snapshot(screen, preset.name(), size);
                        }
                    }
                }
            }
        }
    }
    private static void press(Screen screen, String label) {
        UiButton button = screen.children().stream().filter(UiButton.class::isInstance).map(UiButton.class::cast)
                .filter(widget -> widget.visible && widget.getMessage().getString().equals(label)).findFirst().orElseThrow();
        button.onPress();
    }
    private static void pressPrefix(Screen screen, String label) {
        UiButton button = screen.children().stream().filter(UiButton.class::isInstance).map(UiButton.class::cast)
                .filter(widget -> widget.visible && widget.getMessage().getString().startsWith(label)).findFirst().orElseThrow();
        button.onPress();
    }
    private static void snapshot(Screen screen, String suffix, int[] size) throws Exception {
        try (UiRenderCapture capture = new UiRenderCapture(size[0], size[1])) {
            screen.render(capture.graphics, -1000, -1000, 0);
            capture.save(screen.getClass().getSimpleName() + "-" + suffix + "-" + size[0] + "x" + size[1]);
        }
    }

    @Test void longHudContentStillFitsSmallScreens() {
        var elements = cn.blockforge.generated.generatedmod.client.ClientHudLayout.Elements.defaultsFor(
                cn.blockforge.generated.generatedmod.client.HudContext.MATCHING);
        for (int width : List.of(320, 480, 640, 960)) {
            for (var rect : List.of(cn.blockforge.generated.generatedmod.client.HudGeometry.score(elements, width, 240),
                    cn.blockforge.generated.generatedmod.client.HudGeometry.feed(elements, width, 240, 4000),
                    cn.blockforge.generated.generatedmod.client.HudGeometry.text(elements, width, 240, 4000))) {
                assertTrue(rect.left() >= 0 && rect.right() <= width);
                assertTrue(rect.top() >= 0 && rect.bottom() <= 240);
            }
        }
    }

    @Test void defaultHudKeepsScoreFeedAndStatusSeparate() {
        var elements = cn.blockforge.generated.generatedmod.client.ClientHudLayout.Elements.defaultsFor(
                cn.blockforge.generated.generatedmod.client.HudContext.TEAM_DEATHMATCH);
        for (int[] size : List.of(new int[]{320, 240}, new int[]{480, 270}, new int[]{640, 360}, new int[]{960, 540})) {
            var score = cn.blockforge.generated.generatedmod.client.HudGeometry.score(elements, size[0], size[1]);
            var feed = cn.blockforge.generated.generatedmod.client.HudGeometry.feed(elements, size[0], size[1], 420);
            var status = cn.blockforge.generated.generatedmod.client.HudGeometry.text(elements, size[0], size[1], elements.textWidth());
            assertTrue(score.bottom() < feed.top()); assertTrue(feed.bottom() < status.top());
        }
    }

    @Test void optionMenuCancelsAndCommitsExactlyOneSelection() {
        var changed = new java.util.concurrent.atomic.AtomicInteger(-1);
        var menu = new UiChoicePopup();
        Screen screen = mock(Screen.class); screen.width = 320; screen.height = 240;
        var button = new UiCycleButton<>(200, 210, 100, 22, List.of(10, 20, 30), 10,
                Object::toString, changed::set, null, UiButton.Kind.SECONDARY);
        menu.open(button, screen); assertTrue(menu.key(264)); menu.key(256);
        assertEquals(-1, changed.get());
        menu.open(button, screen); menu.key(264); menu.key(257);
        assertEquals(20, changed.get()); assertEquals(20, button.getValue()); assertFalse(menu.isOpen());
    }
    private static Screen rulesScreen() {
        try {
            var constructor = RoomRulesScreen.class.getDeclaredConstructor(Screen.class);
            constructor.setAccessible(true); return constructor.newInstance((Screen) null);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static void seedData() {
        var rules = cn.blockforge.generated.generatedmod.lobby.RoomRules.fallback();
        var rooms = java.util.stream.IntStream.range(0, 18).mapToObj(i -> new cn.blockforge.generated.generatedmod.lobby.RoomView(
                "room-" + i, "竞技房间 " + (i + 1), "Player" + i, 6, 16, "arena-" + i,
                cn.blockforge.generated.generatedmod.lobby.RoomState.OPEN, List.of("Player0", "Steve", "Alex", "Player3", "Player4", "Player5"), rules, false, i % 3 == 0,
                4, java.util.Map.of("Player0", cn.blockforge.generated.generatedmod.match.Team.TEAM_A,
                        "Steve", cn.blockforge.generated.generatedmod.match.Team.TEAM_B,
                        "Alex", cn.blockforge.generated.generatedmod.match.Team.TEAM_C,
                        "Player3", cn.blockforge.generated.generatedmod.match.Team.TEAM_D,
                        "Player4", cn.blockforge.generated.generatedmod.match.Team.TEAM_A,
                        "Player5", cn.blockforge.generated.generatedmod.match.Team.TEAM_B))).toList();
        cn.blockforge.generated.generatedmod.client.ClientLobbyData.apply(new cn.blockforge.generated.generatedmod.network.packet.RoomSyncPacket(rooms, "room-0", true, false, "", false));
        cn.blockforge.generated.generatedmod.client.ClientLobbyData.apply(new cn.blockforge.generated.generatedmod.network.packet.RoomMapListSyncPacket(
                List.of("arena-0|废弃工厂|Player0", "arena-1|山地要塞|Alex", "arena-2|边境哨站|Steve")));
        var region = new cn.blockforge.generated.generatedmod.map.MapEditorView.RegionData(true, -32, 64, -32, 32, 96, 32);
        var view = new cn.blockforge.generated.generatedmod.map.MapEditorView(true, "arena-0", "废弃工厂", "minecraft:overworld",
                region, region, region, region, 4, 4, 1, "已捕获", true, true, false, false,
                List.of("arena-0|废弃工厂|AB12CD", "arena-1|山地要塞|", "arena-2|边境哨站|XY98ZT"), List.of(), "AB12CD", 0, "", false, 2, 2,
                List.of(), cn.blockforge.generated.generatedmod.map.MapTool.BOUNDS,
                cn.blockforge.generated.generatedmod.map.MapBrushMode.REGION, "bounds", 2);
        cn.blockforge.generated.generatedmod.client.ClientMapEditorData.apply(new cn.blockforge.generated.generatedmod.network.packet.MapEditorSyncPacket(view));
    }
    private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }

    private static final class ScrollTestScreen extends UiScreen {
        private ScrollTestScreen() {
            super(Component.literal("滚动测试"));
        }

        @Override
        protected void init() {
            beginLayout(240, 0, BUTTON_HEIGHT, false, true);
            for (int index = 0; index < 30; index++) {
                int y = flowRow(BUTTON_HEIGHT);
                flowWidget(uiButton("行 " + index, innerLeft, y, innerWidth, () -> { }, null), y);
            }
        }
    }
}
