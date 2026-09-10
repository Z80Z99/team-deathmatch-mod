package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.mixin.PostChainAccessor;
import com.mojang.blaze3d.shaders.AbstractUniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/** Applies the final-five-second grayscale/contrast curve without replacing other camera effects. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoundaryPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_boundary_effect");
    private static PostChain chain;
    private static int width;
    private static int height;
    private static boolean unavailable;

    private BoundaryPostProcessor() { }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        float phase = ClientMatchData.boundaryOutside
                ? BoundaryEffects.finalPhase(ClientMatchData.boundaryTicks) : 0.0F;
        if (phase <= 0.001F) {
            close();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!ensureChain(minecraft)) return;
        AbstractUniform uniform = ((PostChainAccessor) chain).generatedMod$getPasses().get(0)
                .getEffect().safeGetUniform("BoundaryPhase");
        if (uniform != null) uniform.set(phase);
        chain.process(event.getPartialTick());
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    private static boolean ensureChain(Minecraft minecraft) {
        if (unavailable) return false;
        int newWidth = minecraft.getWindow().getWidth();
        int newHeight = minecraft.getWindow().getHeight();
        try {
            if (chain == null) {
                chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(),
                        minecraft.getMainRenderTarget(), new ResourceLocation(GeneratedMod.MOD_ID, "shaders/post/boundary.json"));
            }
            if (width != newWidth || height != newHeight) {
                chain.resize(newWidth, newHeight);
                width = newWidth;
                height = newHeight;
            }
            return true;
        } catch (IOException | RuntimeException error) {
            close();
            unavailable = true;
            LOGGER.warn("Boundary post effect is unavailable; keeping the warning overlays only", error);
            return false;
        }
    }

    private static void close() {
        if (chain != null) chain.close();
        chain = null;
        width = 0;
        height = 0;
    }
}
