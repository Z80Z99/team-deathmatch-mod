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
