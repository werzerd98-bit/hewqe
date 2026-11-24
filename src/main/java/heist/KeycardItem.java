package heist;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Единая ключ-карта: подходит к любому считывателю. */
public class KeycardItem extends Item {
    public KeycardItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Открывает любой считыватель").formatted(Formatting.GREEN));
        tooltip.add(Text.literal("ПКМ по считывателю: открыть. ПКМ ещё раз: закрыть").formatted(Formatting.GRAY));
    }
}
