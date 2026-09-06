package cn.blockforge.generated.generatedmod.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapPreviewTest {
    @Test void samplingRemainsBoundedAcrossTheEntireCoordinateRange() {
        double span = (double) Integer.MAX_VALUE - Integer.MIN_VALUE;
        assertTrue(MapPreview.edgeSteps(span) <= 8);
        assertTrue(MapPreview.edgeSteps(0) >= 1);
        assertEquals(2, MapPreview.edgeSteps(4));
        int maximumSamplesForBothRegions = 2 * 12 * (MapPreview.edgeSteps(span) + 1);
        assertTrue(maximumSamplesForBothRegions <= 216);
    }
}
