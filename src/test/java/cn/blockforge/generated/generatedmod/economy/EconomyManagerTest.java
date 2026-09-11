package cn.blockforge.generated.generatedmod.economy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EconomyManagerTest {
    @BeforeAll static void bootstrap() {
        cn.blockforge.generated.generatedmod.MinecraftTestBootstrap.initialize();
    }

    @Test void formulaSupportsCsStyleArithmeticAndFunctions() {
        assertEquals(3250, MoneyFormula.evaluate("3250", Map.of()));
        assertEquals(3400, MoneyFormula.evaluate("1400 + min(loss_streak, 4) * 500",
                Map.of("loss_streak", 5.0D)));
        assertEquals(330, MoneyFormula.evaluate("kill_bonus * 1.1",
                Map.of("kill_bonus", 300.0D)));
        assertEquals(50, MoneyFormula.evaluate("max(50, damage * 0.25)",
                Map.of("damage", 100.0D)));
        assertEquals(300, MoneyFormula.evaluate("max(200, durability_lost * 3)",
                Map.of("durability_lost", 100.0D)));
    }

    @Test void matchAndGlobalWalletsAreIndependent() {
        EconomyManager manager = manager();
        ServerPlayer player = player();
        manager.beginMatch(List.of(player));

        assertEquals(800, manager.matchBalance(player.getUUID()));
        assertEquals(1000, manager.globalBalance(player.getUUID()));
        assertTrue(manager.purchase(player, "gun_pistol_test", "gun_pistol", 1, true).success());
        assertEquals(500, manager.matchBalance(player.getUUID()));
        assertEquals(1000, manager.globalBalance(player.getUUID()));

        assertTrue(manager.purchase(player, "gun_pistol_test", "gun_pistol", 1, false).success());
        assertEquals(700, manager.globalBalance(player.getUUID()));
    }

    @Test void insufficientFundsDoesNotGrantPurchase() {
        EconomyManager manager = manager();
        ServerPlayer player = player();
        manager.beginMatch(List.of(player));
        manager.setBalance(player.getUUID(), 3000, true);

        assertTrue(manager.purchase(player, "gun_rifle_test", "gun_rifle", 1, true).success());
        assertEquals(300, manager.matchBalance(player.getUUID()));
        EconomyManager.PurchaseResult result =
                manager.purchase(player, "gun_rifle_test_expensive", "gun_rifle", 2, true);
        assertFalse(result.success());
        assertEquals(300, manager.matchBalance(player.getUUID()));
    }

    @Test void abortedMatchRejectsStaleMatchPurchases() {
        EconomyManager manager = manager();
        ServerPlayer player = player();
        manager.beginMatch(List.of(player));
        manager.abortMatch();

        assertFalse(manager.matchActive());
        assertFalse(manager.purchase(player, "gun_pistol_test", "gun_pistol", 1, true).success());
    }

    @Test void durabilityServicePriceUsesLostDurabilityFormula() {
        EconomyManager manager = manager();
        int price = manager.price("service_repair_gun", "service_repair_gun", 1,
                Map.of("durability_lost", 100.0D));
        assertEquals(300, price);
    }

    private EconomyManager manager() {
        Path temp;
        try {
            temp = Files.createTempDirectory("tdm-economy-test-");
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getServerDirectory()).thenReturn(temp.toFile());
        return new EconomyManager(server);
    }

    private static ServerPlayer player() {
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        return player;
    }
}
