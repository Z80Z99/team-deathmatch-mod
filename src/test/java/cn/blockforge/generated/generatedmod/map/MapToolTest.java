package cn.blockforge.generated.generatedmod.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapToolTest {
    @Test
    void parsesEveryToolId() {
        for (MapTool tool : MapTool.values()) {
            assertEquals(tool, MapTool.parse(tool.id()));
        }
    }

    @Test
    void rejectsUnknownToolId() {
        assertNull(MapTool.parse("not_a_tool"));
        assertNotNull(MapTool.parse("bounds"));
    }
}
