package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapToolInventoryTest {
    @BeforeAll static void bootstrap() { MinecraftTestBootstrap.initialize(); }

    @Test void renamedToolInOffhandCannotBeClaimedAgain() {
        Inventory inventory = new Inventory(mock(net.minecraft.world.entity.player.Player.class));
        ItemStack existing = new ItemStack(Items.STICK);
        existing.setHoverName(Component.literal("My tool"));
        inventory.offhand.set(0, existing);
        assertEquals(0, MapEditorManager.giveMissingTool(inventory, ItemStack.EMPTY, Items.STICK));
        assertTrue(inventory.items.stream().allMatch(ItemStack::isEmpty));
    }

    @Test void repeatedClaimOnlyAddsOneTool() {
        Inventory inventory = new Inventory(mock(net.minecraft.world.entity.player.Player.class));
        assertEquals(1, MapEditorManager.giveMissingTool(inventory, ItemStack.EMPTY, Items.STICK));
        assertEquals(0, MapEditorManager.giveMissingTool(inventory, ItemStack.EMPTY, Items.STICK));
        assertEquals(1, inventory.items.stream().mapToInt(ItemStack::getCount).sum());
    }

    @Test void cursorToolAndFullInventoryDoNotCreateExtras() {
        Inventory inventory = new Inventory(mock(net.minecraft.world.entity.player.Player.class));
        assertEquals(0, MapEditorManager.giveMissingTool(inventory, new ItemStack(Items.STICK), Items.STICK));
        for (int i = 0; i < inventory.items.size(); i++) inventory.items.set(i, new ItemStack(Items.STONE, 64));
        assertEquals(-1, MapEditorManager.giveMissingTool(inventory, ItemStack.EMPTY, Items.STICK));
    }
}
