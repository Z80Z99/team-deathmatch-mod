package cn.blockforge.generated.generatedmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 爆破模式使用的拆弹器。 */
public class JammerTabletItem extends Item {
    public JammerTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.generated_mod.jammer_tablet.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
