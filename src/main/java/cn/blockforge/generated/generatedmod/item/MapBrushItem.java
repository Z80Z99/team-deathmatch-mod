package cn.blockforge.generated.generatedmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 地图画笔：左键端点 A / 移除，右键端点 B / 添加，蹲下右键切换模式。 */
public class MapBrushItem extends Item {
    public MapBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.generated_mod.map_brush.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
