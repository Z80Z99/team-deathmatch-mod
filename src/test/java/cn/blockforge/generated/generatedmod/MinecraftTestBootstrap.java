package cn.blockforge.generated.generatedmod;

/** Load vanilla registries without Forge's launch-time transformed network event bus. */
public final class MinecraftTestBootstrap {
    private static boolean initialized;
    private MinecraftTestBootstrap() { }
    public static synchronized void initialize() {
        if (initialized) return;
        net.minecraft.SharedConstants.tryDetectVersion();
        try (var network = org.mockito.Mockito.mockStatic(net.minecraftforge.network.NetworkHooks.class)) {
            net.minecraft.server.Bootstrap.bootStrap();
        }
        initialized = true;
    }
}
