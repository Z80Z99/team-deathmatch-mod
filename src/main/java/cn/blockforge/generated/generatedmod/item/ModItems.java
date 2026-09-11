package cn.blockforge.generated.generatedmod.item;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 本 Mod 的道具注册表。 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GeneratedMod.MOD_ID);

    public static final RegistryObject<MapPlannerItem> MAP_PLANNER =
            ITEMS.register("map_planner", () -> new MapPlannerItem(new Item.Properties()));
    public static final RegistryObject<MapBrushItem> MAP_BRUSH =
            ITEMS.register("map_brush", () -> new MapBrushItem(new Item.Properties()));
    public static final RegistryObject<C4Item> C4 =
            ITEMS.register("c4", () -> new C4Item(new Item.Properties()));
    public static final RegistryObject<JammerTabletItem> JAMMER_TABLET =
            ITEMS.register("jammer_tablet", () -> new JammerTabletItem(new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MAP_PLANNER);
            event.accept(MAP_BRUSH);
            event.accept(C4);
            event.accept(JAMMER_TABLET);
        }
    }
}
