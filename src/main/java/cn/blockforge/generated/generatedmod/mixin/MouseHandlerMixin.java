package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.client.RespawnOverlay;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the death observer camera still without fighting mouse rotation over the network. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void generatedMod$lockDeathObserverView(LocalPlayer player, double horizontal, double vertical) {
        if (!RespawnOverlay.shouldLockViewInput()) {
            player.turn(horizontal, vertical);
        }
    }
}
