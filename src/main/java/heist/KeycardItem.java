package heist;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KeycardItem extends Item {
    private final int tier;

    public KeycardItem(Settings settings, int tier) {
        super(settings);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Formatting color = tier == 1 ? Formatting.GREEN : tier == 2 ? Formatting.BLUE : Formatting.RED;
        tooltip.add(Text.literal("Уровень доступа: " + tier).formatted(color));
        tooltip.add(Text.literal("ПКМ по считывателю").formatted(Formatting.GRAY));
    }
}
