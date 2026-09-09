package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.client.RespawnOverlay;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Consumes death-camera look input without repeatedly correcting player rotation. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Redirect(method = "m_91523_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;m_19884_(DD)V", remap = false),
            require = 0, remap = false)
    private void generatedMod$lockDeathObserverView(LocalPlayer player, double horizontal, double vertical) {
        if (!RespawnOverlay.shouldLockViewInput()) player.turn(horizontal, vertical);
    }
}
