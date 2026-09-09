package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelBoundaryMixin {
    @Inject(method = {"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            "m_6933_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"},
            at = @At("HEAD"), cancellable = true, remap = false)
    private void tdm$protect(BlockPos pos, BlockState state, int flags, int recursion,
                             CallbackInfoReturnable<Boolean> ci) {
        if ((Object) this instanceof ServerLevel level) {
            MatchManager match = MatchManager.get();
            if (match != null && !cn.blockforge.generated.generatedmod.map.ResetWriteAccess.active()
                    && match.shouldProtectOutside(level, pos)) ci.setReturnValue(false);
        }
    }
}
