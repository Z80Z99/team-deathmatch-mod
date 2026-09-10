package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
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
import java.lang.reflect.Field;
import java.util.List;

/** Applies the final-five-second grayscale/contrast curve without replacing other camera effects. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoundaryPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_boundary_effect");
    private static PostChain chain;
    private static int width;
    private static int height;
    private static boolean unavailable;
    private static AbstractUniform phaseUniform;

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
        phaseUniform.set(phase);
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
                phaseUniform = findPhaseUniform(chain);
                if (phaseUniform == null) throw new IllegalStateException("BoundaryPhase uniform was not found");
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

    /** Locate the pass list by value type so this stays stable across Mojang/SRG field names. */
    private static AbstractUniform findPhaseUniform(PostChain postChain) {
        try {
            for (Field field : PostChain.class.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType()) || !field.trySetAccessible()) continue;
                Object value = field.get(postChain);
                if (!(value instanceof List<?> list)) continue;
                for (Object entry : list) {
                    if (entry instanceof net.minecraft.client.renderer.PostPass pass) {
                        AbstractUniform uniform = pass.getEffect().safeGetUniform("BoundaryPhase");
                        if (uniform != null) return uniform;
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // The caller disables only this optional visual effect.
        }
        return null;
    }

    private static void close() {
        if (chain != null) chain.close();
        chain = null;
        phaseUniform = null;
        width = 0;
        height = 0;
    }
}
