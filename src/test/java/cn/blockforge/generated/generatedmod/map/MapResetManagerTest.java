package cn.blockforge.generated.generatedmod.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapResetManagerTest {
    @Test void rejectedWriteWithWrongStateFailsTheRestore() {
        assertFalse(MapResetManager.blockStateWasRestored(false, false));
    }

    @Test void matchingStateIsAcceptedWhenSetBlockReportsNoChange() {
        assertTrue(MapResetManager.blockStateWasRestored(false, true));
    }

    @Test void changedStateIsAccepted() {
        assertTrue(MapResetManager.blockStateWasRestored(true, false));
    }
}
