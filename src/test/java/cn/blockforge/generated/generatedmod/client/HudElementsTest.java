package cn.blockforge.generated.generatedmod.client;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HudElementsTest {
    @Test void globalHudContextHasDedicatedElementsAndAllSources() {
        var global = ClientHudLayout.Elements.defaultsFor(HudContext.GLOBAL);
        assertEquals(0, global.builtInMask());
        assertTrue(HudStats.sourcesFor(HudContext.GLOBAL).stream()
                .anyMatch(source -> source.id().equals("score_a")));
        assertTrue(HudStats.sourcesFor(HudContext.GLOBAL).stream()
                .anyMatch(source -> source.id().equals("room_name")));
    }

    @Test void progressAndContentEffectsSurviveJsonRoundTrip() {
        ClientHudLayout.Placement placement = ClientHudLayout.Placement.NONE
                .withContentAnimation("slide", 600)
                .withProgressDirection("drain");
        ClientHudLayout.CustomElement element = new ClientHudLayout.CustomElement(
                "effect_test", "progress", "{round}", 25, 75, 320, 18, 90,
                -1, true, "score_a", 120, true, true, true, placement);
        ClientHudLayout.CustomElement restored = ClientHudLayout.CustomElement.read(element.toJson());
        assertEquals("slide", restored.placement().contentAnimation());
        assertEquals(600, restored.placement().contentAnimationMillis());
        assertEquals("drain", restored.placement().progressDirection());
    }

    @Test void dataSourcesExposeThreeLevelCategories() {
        HudStats.Source score = HudStats.byId("score_a");
        assertEquals("得分", HudStats.primaryCategory(score));
        assertEquals("本轮", HudStats.secondaryCategory(score));
        assertEquals("A队", HudStats.tertiaryCategory(score));
        HudStats.Source kills = HudStats.byId("match_kills_a");
        assertEquals("击杀数", HudStats.primaryCategory(kills));
        assertEquals("整场", HudStats.secondaryCategory(kills));
        assertEquals("局内", HudStats.secondaryCategory(HudStats.byId("my_match_money")));
        assertEquals("局外", HudStats.secondaryCategory(HudStats.byId("my_global_money")));
        assertTrue(HudStats.byId("my_global_money").isLive());
    }

    @Test void sceneSourcesAndRespawnProgressAreConsistent() {
        assertTrue(HudStats.sourcesFor(HudContext.TEAM_DEATHMATCH).stream().noneMatch(s -> s.group() == HudStats.Group.ROOM || s.group() == HudStats.Group.MATCHING));
        assertTrue(HudStats.sourcesFor(HudContext.TEAM_DEATHMATCH).stream()
                .noneMatch(s -> s.id().equals("bomb_countdown")));
        assertTrue(HudStats.sourcesFor(HudContext.SEARCH_DESTROY).stream()
                .anyMatch(s -> s.id().equals("bomb_countdown")));
        assertTrue(HudStats.sourcesFor(HudContext.ROOM).stream().noneMatch(s -> s.group() == HudStats.Group.SCORE));
        ClientMatchData.state = cn.blockforge.generated.generatedmod.match.MatchState.PLAYING;
        ClientMatchData.respawnTotalTicks = 200;
        ClientMatchData.respawnRemainingTicks = 80;
        ClientMatchData.phaseTotalTicks = 600 * 20;
        ClientMatchData.matchTotalTicks = 600 * 20;
        ClientMatchData.boundaryTicks = 160;
        assertEquals(.4, HudStats.byId("respawn_left").ratio(false), .0001);
        assertEquals("00:08", HudStats.byId("boundary_remaining").display(false));
        assertTrue(HudStats.sourcesFor(HudContext.TEAM_DEATHMATCH).stream()
                .anyMatch(source -> source.id().equals("boundary_remaining")));
        assertTrue(HudParameters.visible("team_d", true));
    }

    @Test void timeSourcesHaveUsableProgressMaximaAndBombTicksSmoothly() {
        ClientMatchData.state = cn.blockforge.generated.generatedmod.match.MatchState.PLAYING;
        ClientMatchData.mode = cn.blockforge.generated.generatedmod.match.GameMode.SEARCH_DESTROY;
        ClientMatchData.respawnTotalTicks = 200;
        ClientMatchData.respawnRemainingTicks = 80;
        ClientMatchData.phaseRemainingTicks = 45 * 20;
        ClientMatchData.phaseTotalTicks = 600 * 20;
        ClientMatchData.matchTotalTicks = 600 * 20;
        ClientMatchData.boundaryTicks = 160;
        ClientBombData.active = true;
        ClientBombData.phase = cn.blockforge.generated.generatedmod.match.ClassicBombState.Phase.PLANTED;
            ClientBombData.bombSiteName = "A";
            ClientBombData.detonationRemainingTicks = 200;
            ClientBombData.detonationTotalTicks = 800;
            try {
                assertEquals(800, HudStats.byId("bomb_countdown").maximum(false));
                assertEquals(.25, HudStats.byId("bomb_countdown").ratio(false), .0001);
            assertEquals(12000, HudStats.byId("phase_remaining").maximum(false));
            assertEquals(.075, HudStats.byId("phase_remaining").ratio(false), .0001);
            assertEquals(200, HudStats.byId("boundary_remaining").maximum(false));
            assertEquals(.8, HudStats.byId("boundary_remaining").ratio(false), .0001);
            assertTrue(MatchHudOverlay.hintText().contains("10.0"), MatchHudOverlay.hintText());
            ClientBombData.tick();
            ClientBombData.tick();
            ClientBombData.tick();
            assertEquals(197, ClientBombData.detonationRemainingTicks);
        } finally {
            ClientBombData.clear();
            ClientMatchData.state = cn.blockforge.generated.generatedmod.match.MatchState.WAITING;
            ClientMatchData.mode = cn.blockforge.generated.generatedmod.match.GameMode.TEAM_DEATHMATCH;
            ClientMatchData.respawnTotalTicks = 0;
            ClientMatchData.respawnRemainingTicks = 0;
            ClientMatchData.phaseRemainingTicks = 0;
            ClientMatchData.boundaryTicks = 0;
        }
    }

    @Test void noticeSourcesMirrorWarmupAndBombEvents() {
        var previousState = ClientMatchData.state;
        var previousMode = ClientMatchData.mode;
        int previousPhase = ClientMatchData.phaseRemainingTicks;
        try {
            ClientMatchData.state = cn.blockforge.generated.generatedmod.match.MatchState.WARMUP;
            ClientMatchData.mode = cn.blockforge.generated.generatedmod.match.GameMode.TEAM_DEATHMATCH;
            ClientMatchData.phaseRemainingTicks = 600;
            assertEquals("热身阶段", MatchHudNotice.title());
            assertTrue(MatchHudNotice.detail().contains("30 秒后开始"));
            assertEquals("30 秒", MatchHudNotice.timer());
            assertTrue(HudStats.sourcesFor(HudContext.TEAM_DEATHMATCH).stream()
                    .anyMatch(source -> source.id().equals("notice_title")));

            ClientBombData.active = true;
            ClientBombData.phase = cn.blockforge.generated.generatedmod.match.ClassicBombState.Phase.PLANTED;
            ClientBombData.bombSiteName = "A";
            ClientBombData.detonationRemainingTicks = 200;
            assertEquals("C4 已安装", MatchHudNotice.title());
            assertTrue(MatchHudNotice.detail().contains("A"));
            assertEquals("10.0 秒", MatchHudNotice.timer());
        } finally {
            ClientBombData.clear();
            ClientMatchData.state = previousState;
            ClientMatchData.mode = previousMode;
            ClientMatchData.phaseRemainingTicks = previousPhase;
        }
    }

    @Test void everyMatchPresetContainsCombatReadouts() {
        for (var preset : HudPreset.values()) {
            var draft = draft();
            preset.apply(draft, HudContext.TEAM_DEATHMATCH);
            String content = draft.customElements(HudContext.TEAM_DEATHMATCH).stream()
                    .map(element -> element.text() + "|" + element.source() + "|"
                            + element.placement().condition())
                    .collect(java.util.stream.Collectors.joining("\n"));
            for (String required : List.of("notice_title", "notice_detail", "notice_timer",
                    "target", "health_percent", "held_weapon", "held_ammo", "held_durability",
                    "my_kills", "my_deaths", "my_damage", "my_match_money", "match_kills_sum",
                    "bomb_phase", "bomb_countdown")) {
                assertTrue(content.contains(required), preset.label() + " 缺少 " + required);
            }
        }
    }

    @Test void structuredEventsDriveSpecificHudSourcesAndThresholdTriggers() {
        int previousMoney = ClientMatchData.matchBalance;
        try {
            ClientHudEventData.apply(new cn.blockforge.generated.generatedmod.network.packet.HudEventPacket(
                    cn.blockforge.generated.generatedmod.match.MatchHudEventType.BUY_PHASE_START,
                    "购买并准备装备", 100));
            assertTrue(HudParameters.visible("event:buy_phase_start", false));
            assertFalse(HudParameters.visible("event:action_phase", false));
            assertEquals("购买阶段开始",
                    HudStats.byId("event:buy_phase_start:title").display(false));
            assertEquals("购买并准备装备",
                    HudStats.byId("event:buy_phase_start:detail").display(false));

            ClientMatchData.matchBalance = 500;
            assertTrue(HudParameters.visible("money_below:1000", false));
            assertFalse(HudParameters.visible("money_at_least:1000", false));
            ClientMatchData.myMatchKills = 10;
            assertTrue(HudParameters.visible("kills_at_least:10", false));
        } finally {
            ClientHudEventData.clear();
            ClientMatchData.matchBalance = previousMoney;
            ClientMatchData.myMatchKills = 0;
        }
    }

    @Test void externalEventsRegisterSourcesAndConditions() {
        HudApi.registerEvent("test:objective", "目标推进", "进入目标区域", 80, 70);
        try {
            assertTrue(ClientHudEventData.descriptors().stream()
                    .anyMatch(descriptor -> descriptor.id().equals("test:objective")));
            assertTrue(HudStats.sourcesFor(HudContext.TEAM_DEATHMATCH).stream()
                    .anyMatch(source -> source.id().equals("event:test:objective:title")));
            ClientHudEventData.apply(new cn.blockforge.generated.generatedmod.network.packet
                    .HudEventPacket("test:objective", "目标推进", "进入目标区域", 80, 70));
            assertTrue(HudParameters.visible("event:test:objective", false));
            assertEquals("目标推进",
                    HudStats.byId("event:test:objective:title").display(false));
        } finally {
            ClientHudEventData.unregister("test:objective");
            ClientHudEventData.clear();
        }
    }

    private ClientHudLayout.CustomElement conditional(String id, String condition) {
        return new ClientHudLayout.CustomElement(id, "text", "test", 50, 50, 100, 20, 100, -1, true,
                "", 100, false, false, false, new ClientHudLayout.Placement(0, 0, 0, 0, "", condition));
    }

    @Test void dependenciesRejectCyclesAndAllowEditorInspection() {
        var a = conditional("a", "hidden:b"); var b = conditional("b", "hidden:a");
        var both = java.util.List.of(a, b);
        assertFalse(HudConditions.visible(a, both, false));
        assertFalse(HudConditions.visible(b, both, false));
        assertTrue(HudConditions.visible(a, both, true));
        assertTrue(HudConditions.visible(a, java.util.List.of(a), false));
        HudApi.registerCondition("api:test", () -> false);
        try { assertFalse(HudConditions.visible(conditional("c", "api:test"), java.util.List.of(), false)); }
        finally { HudApi.unregisterCondition("api:test"); }
    }

    @Test void animationReversesAndSettingsRoundTrip() {
        assertEquals(.5, HudAnimation.advance(0, true, 125, 250), .0001);
        assertEquals(.25, HudAnimation.advance(.5, false, 62.5, 250), .0001);
        var placement = new ClientHudLayout.Placement(1, 2, 640, 360, "", "team_d")
                .withAnimation("slide", 450).withMaximum(60);
        var element = new ClientHudLayout.CustomElement("animated", "progress", "%s", 50, 50, 100, 20, 80,
                -1, true, "respawn_left", 100, true, false, false, placement);
        assertEquals(element, ClientHudLayout.CustomElement.read(element.toJson()));
    }

    @Test void alignmentAndDecorationColorsRoundTrip() {
        var placement = new ClientHudLayout.Placement(0, 0, 0, 0, "", "")
                .withAlignment("right").withColors(0x112233, 0x445566, 0x778899, 0xAABBCC).withGlow(true);
        var element = new ClientHudLayout.CustomElement("decorated", "text", "A队 {match_kills_a}",
                100, 50, 180, 24, 80, 0xFFFFFF, true, "", 100, true, true, true, placement);
        assertEquals("right", placement.alignment());
        assertTrue(placement.glow());
        assertEquals(element, ClientHudLayout.CustomElement.read(element.toJson()));
    }

    @Test void statusEffectKindsAreIndependentSerializableComponents() {
        var respawn = ClientHudLayout.CustomElement.respawn("respawn_test");
        var boundary = ClientHudLayout.CustomElement.boundary("boundary_test");
        assertEquals("respawning", respawn.placement().condition());
        assertEquals("outside", boundary.placement().condition());
        assertTrue(respawn.displayName().startsWith("阵亡与部署"));
        assertTrue(boundary.displayName().startsWith("越界警告"));
        assertEquals(respawn, ClientHudLayout.CustomElement.read(respawn.toJson()));
        assertEquals(boundary, ClientHudLayout.CustomElement.read(boundary.toJson()));
    }
    private MockedStatic<Minecraft> access;

    @BeforeEach void setup() throws Exception {
        Minecraft minecraft = mock(Minecraft.class);
        Font font = mock(Font.class);
        var field = Minecraft.class.getDeclaredField("font"); field.setAccessible(true); field.set(minecraft, font);
        when(font.width(anyString())).thenAnswer(call -> ((String) call.getArgument(0)).codePoints()
                .map(c -> c > 255 ? 9 : 5).sum());
        access = mockStatic(Minecraft.class);
        access.when(Minecraft::getInstance).thenReturn(minecraft);
        ClientMatchData.clear();
        ClientLobbyData.clear();
    }

    @AfterEach void cleanup() { access.close(); ClientMatchData.clear(); ClientLobbyData.clear(); }

    private ClientHudLayout.Draft draft() {
        return new ClientHudLayout.Snapshot(ClientHudLayout.Global.defaults(), Map.of(), Map.of()).draft();
    }

    @Test void namedParametersOnlySubstituteValuesAndNeverAppend() {
        assertEquals("1", HudParameters.render("{round}", true));
        assertEquals("回合 1", HudParameters.render("回合 {round}", true));
        assertEquals("目标 25", HudParameters.render("目标 {target}", true));
        assertEquals("A队 114", HudStats.applyTemplate("A队 114", "12", "12", "25"));
        assertEquals("", HudStats.applyTemplate("", "12", "12", "25"));
        assertEquals("HP 12/25", HudStats.applyTemplate("HP %v/%m", "12", "12", "25"));
        assertEquals("{victim}", HudStats.resolveTemplate("{killer}", Map.of("killer", "{victim}", "victim", "Alex")));
        assertEquals("{unknown}", HudParameters.render("{unknown}", true));
    }

    @Test void oldTemplatesMigrateOnceWithoutDuplicatingLabels() {
        var object = JsonParser.parseString("""
                {"version":5,"contexts":{"match:TEAM_DEATHMATCH":{"scoreDetailsTemplate":"{round}  {target}",
                "scoreHeaderTemplate":"回合 {round}"}},"customElements":{"match:TEAM_DEATHMATCH":[
                {"source":"my_kills","text":"击杀"},{"source":"health","text":"HP %s"}]}}
                """).getAsJsonObject();
        ClientHudLayout.migrateTemplates(object);
        var fields = object.getAsJsonObject("contexts").getAsJsonObject("match:TEAM_DEATHMATCH");
        assertEquals("回合 {round}  目标 {target}", fields.get("scoreDetailsTemplate").getAsString());
        assertEquals("回合 {round}", fields.get("scoreHeaderTemplate").getAsString());
        var modules = object.getAsJsonObject("customElements").getAsJsonArray("match:TEAM_DEATHMATCH");
        assertEquals("击杀 %s", modules.get(0).getAsJsonObject().get("text").getAsString());
        assertEquals("HP %s", modules.get(1).getAsJsonObject().get("text").getAsString());
        var copy = object.deepCopy();
        ClientHudLayout.migrateTemplates(object);
        assertEquals(copy, object);
    }

    @Test void everyBuiltInBecomesIndependentlyDeletableAndStaysDeleted() {
        var draft = draft();
        for (var context : HudContext.values()) {
            if (context == HudContext.GLOBAL) {
                assertEquals(0, draft.elements(context).builtInMask());
                assertTrue(draft.customElements(context).isEmpty());
                continue;
            }
            HudAssemblies.splitAll(draft, context);
            assertEquals(0, draft.elements(context).builtInMask());
            assertTrue(draft.customElements(context).size() > 4);
            var first = draft.customElements(context).get(0);
            int count = draft.customElements(context).size();
            draft.removeCustomElement(context, first.id());
            HudAssemblies.splitAll(draft, context);
            assertEquals(count - 1, draft.customElements(context).size());
            assertFalse(draft.customElements(context).contains(first));
            for (var element : draft.customElements(context)) assertEquals(element, ClientHudLayout.CustomElement.read(element.toJson()));
            for (var element : List.copyOf(draft.customElements(context))) draft.removeCustomElement(context, element.id());
            HudAssemblies.splitAll(draft, context);
            assertTrue(draft.customElements(context).isEmpty());
        }
    }

    @Test void presetsAreAtomicRepeatableAndPreserveUserElements() {
        for (var preset : HudPreset.values()) for (var context : HudContext.values()) {
            if (context == HudContext.GLOBAL) continue;
            var draft = draft();
            var custom = ClientHudLayout.CustomElement.text("user-owned");
            draft.addCustomElement(context, custom);
            preset.apply(draft, context);
            var first = draft.build();
            assertNull(first.validationError());
            assertEquals(0, first.elements(context).builtInMask());
            assertTrue(first.customElements(context).contains(custom));
            assertTrue(first.customElements(context).size() > 8);
            preset.apply(draft, context);
            assertEquals(first, draft.build());
            for (int[] size : List.of(new int[]{320,240}, new int[]{480,270}, new int[]{640,360}, new int[]{960,540})) {
                for (var element : first.customElements(context)) {
                    var rect = HudGeometry.custom(element, size[0], size[1]);
                    assertTrue(rect.left() >= 0 && rect.right() <= size[0] && rect.top() >= 0 && rect.bottom() <= size[1], element.id());
                }
            }
        }
    }

    @Test void combatConditionsDoNotShowFakeTeamsOrPersistentKillFeed() {
        assertFalse(HudParameters.visible("feed", false));
        assertTrue(HudParameters.visible("feed", true));
        assertFalse(HudParameters.visible("team_c", false));
        assertFalse(HudParameters.visible("team_d", false));
        assertTrue(HudParameters.visible("", false));
        RespawnOverlay.onDeathScreen();
        assertTrue(HudParameters.visible("death", false));
        assertFalse(HudParameters.visible("respawn_ready", false));
        RespawnOverlay.clear();
    }

    @Test void deletedBuiltInsAreNotDrawnByLiveOverlays() throws Exception {
        var draft = draft();
        for (var context : HudContext.values()) for (var component : HudContext.BuiltIn.values()) {
            draft.of(context).setBuiltInEnabled(component, false);
        }
        var snapshot = draft.build();
        var graphics = mock(net.minecraft.client.gui.GuiGraphics.class);
        var forge = mock(net.minecraftforge.client.gui.overlay.ForgeGui.class);
        var minecraft = Minecraft.getInstance();
        when(forge.getMinecraft()).thenReturn(minecraft);
        try (MockedStatic<ClientHudLayout> layouts = mockStatic(ClientHudLayout.class)) {
            layouts.when(ClientHudLayout::global).thenReturn(snapshot.global());
            for (var context : HudContext.values()) {
                layouts.when(() -> ClientHudLayout.elements(context)).thenReturn(snapshot.elements(context));
                layouts.when(() -> ClientHudLayout.customElements(context)).thenReturn(List.of());
            }
            ClientMatchData.state = cn.blockforge.generated.generatedmod.match.MatchState.WARMUP;
            MatchHudOverlay.render(forge, graphics, 0, 640, 360);
            var method = SceneHudOverlay.class.getDeclaredMethod("renderMatching", net.minecraft.client.gui.GuiGraphics.class,
                    float.class, int.class, int.class, cn.blockforge.generated.generatedmod.lobby.MatchmakingStatus.class,
                    boolean.class, cn.blockforge.generated.generatedmod.lobby.RoomView.class);
            method.setAccessible(true);
            method.invoke(null, graphics, 0F, 640, 360, ClientLobbyData.matchmaking(), false, null);
            verifyNoInteractions(graphics);
        }
    }
}
