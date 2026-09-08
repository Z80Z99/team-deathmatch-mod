package cn.blockforge.generated.generatedmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 地图规划器：蹲下右键打开工具菜单。 */
public class MapPlannerItem extends Item {
    public MapPlannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.generated_mod.map_planner.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
