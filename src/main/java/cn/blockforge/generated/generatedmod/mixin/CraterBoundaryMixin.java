package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Both direct-section and ordinary writes in Explosion Overhaul check chunk availability first. */
@Pseudo
@Mixin(targets = "com.vinlanx.explosionoverhaul.AsyncCraterManager$Job", remap = false)
public abstract class CraterBoundaryMixin {
    @Shadow(remap = false) private int appliedIndex;
    @Redirect(method = "applyBatch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;m_46749_(Lnet/minecraft/core/BlockPos;)Z", ordinal = 0), require = 0)
    private boolean tdm$protectProduction(ServerLevel level, BlockPos pos) { return allowed(level, pos); }

    @Redirect(method = "applyBatch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;hasChunkAt(Lnet/minecraft/core/BlockPos;)Z", ordinal = 0), require = 0)
    private boolean tdm$protectDevelopment(ServerLevel level, BlockPos pos) { return allowed(level, pos); }

    @Redirect(method = "applyBatch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;m_46749_(Lnet/minecraft/core/BlockPos;)Z", ordinal = 1), require = 0)
    private boolean tdm$directProduction(ServerLevel level, BlockPos pos) { return directAllowed(level, pos); }

    @Redirect(method = "applyBatch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;hasChunkAt(Lnet/minecraft/core/BlockPos;)Z", ordinal = 1), require = 0)
    private boolean tdm$directDevelopment(ServerLevel level, BlockPos pos) { return directAllowed(level, pos); }

    private boolean directAllowed(ServerLevel level, BlockPos pos) {
        MatchManager match = MatchManager.get();
        if (match != null && match.shouldProtectBlock(level, pos)) {
            // The grouped-write branch advances only accepted candidates; count protected ones as consumed.
            appliedIndex++;
            return false;
        }
        return level.hasChunkAt(pos);
    }

    private static boolean allowed(ServerLevel level, BlockPos pos) {
        MatchManager match = MatchManager.get();
        return (match == null || !match.shouldProtectBlock(level, pos)) && level.hasChunkAt(pos);
    }
}
