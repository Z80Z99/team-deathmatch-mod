package cn.blockforge.generated.generatedmod.match;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BoundaryCountdownTest {
    @Test void returningCancelsAndAnotherExitGetsFullCountdown() {
        var countdown = new BoundaryCountdown();
        var player = UUID.randomUUID();
        assertEquals(200, countdown.update(player, true, 100));
        assertEquals(1, countdown.update(player, true, 299));
        assertEquals(0, countdown.update(player, true, 300));
        assertEquals(0, countdown.update(player, false, 301));
        assertEquals(200, countdown.update(player, true, 302));
        countdown.clear();
        assertEquals(0, countdown.remaining(player, 303));
    }
    @Test void playersHaveIndependentDeadlines() {
        var countdown = new BoundaryCountdown();
        var a = UUID.randomUUID(); var b = UUID.randomUUID();
        countdown.update(a, true, 0);
        countdown.update(b, true, 50);
        assertEquals(0, countdown.remaining(a, 200));
        assertEquals(50, countdown.remaining(b, 200));
    }
}
