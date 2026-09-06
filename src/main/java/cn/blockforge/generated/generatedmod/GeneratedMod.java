package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.config.FpsTdmConfig;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "generated_mod";
    public static final String MOD_NAME = "团队死斗";

    public GeneratedMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FpsTdmConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(GeneratedMod::commonSetup);
        MinecraftForge.EVENT_BUS.register(cn.blockforge.generated.generatedmod.event.MatchEvents.class);
        MinecraftForge.EVENT_BUS.register(cn.blockforge.generated.generatedmod.command.FpsCommand.class);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FpsTdmNetwork::register);
    }
}
