package heist;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CodeNoteItem extends Item {
    public CodeNoteItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("Code")) {
            tooltip.add(Text.literal("Код: " + nbt.getString("Code")).formatted(Formatting.YELLOW));
            if (nbt.contains("Hint")) {
                tooltip.add(Text.literal(nbt.getString("Hint")).formatted(Formatting.GRAY));
            }
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtCompound nbt = stack.getNbt();
        if (!world.isClient && nbt != null && nbt.contains("Code")) {
            String hint = nbt.contains("Hint") ? " (" + nbt.getString("Hint") + ")" : "";
            user.sendMessage(Text.literal("Записка: код " + nbt.getString("Code") + hint).formatted(Formatting.YELLOW), true);
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
