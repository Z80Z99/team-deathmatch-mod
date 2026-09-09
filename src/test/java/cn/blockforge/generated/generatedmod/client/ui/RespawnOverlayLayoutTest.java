package cn.blockforge.generated.generatedmod.client.ui;

import cn.blockforge.generated.generatedmod.client.ClientMatchData;
import cn.blockforge.generated.generatedmod.client.RespawnOverlay;
import cn.blockforge.generated.generatedmod.client.RespawnTimeline;
import cn.blockforge.generated.generatedmod.match.GameMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RespawnOverlayLayoutTest {
    @Test void deathDetailsFitSmallAndLargeGuiScales() throws Exception {
        Minecraft mc = mock(Minecraft.class);
        Font font = mock(Font.class);
        var gameDirectory = Minecraft.class.getDeclaredField("gameDirectory");
        gameDirectory.setAccessible(true);
        gameDirectory.set(mc, new java.io.File("build/test-game-respawn-layout"));
        var field = Minecraft.class.getDeclaredField("font");
        field.setAccessible(true); field.set(mc, font);
        when(font.width(anyString())).thenAnswer(call -> UiRenderCapture.measure(call.getArgument(0)));
        when(font.plainSubstrByWidth(anyString(), anyInt())).thenAnswer(call ->
                UiRenderCapture.shorten(call.getArgument(0), call.getArgument(1), false));
        var timelineField = RespawnOverlay.class.getDeclaredField("TIMELINE");
        timelineField.setAccessible(true);
        var timeline = (RespawnTimeline) timelineField.get(null);
        try (var minecraft = mockStatic(Minecraft.class)) {
            minecraft.when(Minecraft::getInstance).thenReturn(mc);
            ClientMatchData.deathLabel = "击杀者  VeryLongPlayerName_ForLayoutValidation";
            ClientMatchData.respawnTotalTicks = 100;
            for (int[] size : new int[][] {{160, 90}, {320, 180}, {640, 360}}) {
                for (int state = 0; state < 3; state++) {
                    timeline.clear(); timeline.update(true, true, false, 0);
                    ClientMatchData.mode = state == 2 ? GameMode.SEARCH_DESTROY : GameMode.TEAM_DEATHMATCH;
                    ClientMatchData.respawnRemainingTicks = state == 0 ? 60 : state == 1 ? 0 : Integer.MAX_VALUE;
                    try (var capture = new UiRenderCapture(size[0], size[1])) {
                        RespawnOverlay.render(null, capture.graphics, 0.5F, size[0], size[1]);
                        int labels = 0;
                        for (var call : mockingDetails(capture.graphics).getInvocations()) {
                            Object[] args = call.getArguments();
                            if (call.getMethod().getName().equals("drawString")) {
                                String text = (String) args[1];
                                int x = (int) args[2], y = (int) args[3];
                                assertTrue(x >= 0 && x + UiRenderCapture.measure(text) <= size[0], text);
                                assertTrue(y >= 0 && y + 9 <= size[1], text);
                                assertFalse(text.contains("107374183"));
                                labels++;
                            }
                        }
                        assertEquals(state == 0 ? 4 : 3, labels);
                        capture.save("respawn-" + size[0] + "-" + state);
                    }
                }
            }
        } finally {
            RespawnOverlay.clear();
            ClientMatchData.clear();
        }
    }
}
