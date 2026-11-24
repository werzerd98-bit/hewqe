package heist;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Отмычка: ПКМ по запертой двери или кодовому сейфу запускает мини-игру взлома. */
public class LockpickItem extends Item {
    public LockpickItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("ПКМ по запертой двери или сейфу — взлом").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Три срыва — отмычка ломается").formatted(Formatting.GRAY));
    }
}
