package cn.blockforge.generated.generatedmod.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryEffectsTest {
    @Test void filtersFollowTheTenSecondTimeline() {
        assertEquals(0.0F, BoundaryEffects.redOpacity(200), 0.001F);
        assertEquals(0.25F, BoundaryEffects.redOpacity(160), 0.001F);
        assertEquals(0.0F, BoundaryEffects.blackOpacity(160), 0.001F);
        assertTrue(BoundaryEffects.blackOpacity(80) > 0.0F);
        assertEquals(0.50F, BoundaryEffects.blackOpacity(0), 0.001F);
        assertEquals(0.0F, BoundaryEffects.grayOpacity(101), 0.001F);
        assertTrue(BoundaryEffects.grayOpacity(80) > 0.0F);
        assertEquals(0.42F, BoundaryEffects.grayOpacity(0), 0.001F);
    }

    @Test void cameraShakeBeginsOnlyAfterThirtyPercent() {
        assertEquals(0.0F, BoundaryEffects.shake(140), 0.001F);
        assertTrue(BoundaryEffects.shake(139) > 0.0F);
        assertEquals(1.55F, BoundaryEffects.shake(0), 0.001F);
    }
}
