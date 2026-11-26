package heist;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
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

/**
 * Сумка с добычей (как в PAYDAY 2). Носится в спец-слоте — левой руке, замедляет игрока.
 * Хранит один вид добычи: деньги или золото. ПКМ — бросить сумку, Shift+ПКМ — распаковать в предметы.
 */
public class LootBagItem extends Item {
    public static final String KIND_MONEY = "money";
    public static final String KIND_GOLD = "gold";
    public static final int CAPACITY = 1;

    public LootBagItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(String kind, int units) {
        ItemStack stack = new ItemStack(ModItems.LOOT_BAG);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString("Kind", kind);
        nbt.putInt("Units", units);
        nbt.putInt("CustomModelData", KIND_GOLD.equals(kind) ? 2 : 1);
        return stack;
    }

    public static String kind(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains("Kind") ? nbt.getString("Kind") : KIND_MONEY;
    }

    public static int units(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getInt("Units") : 0;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        boolean gold = KIND_GOLD.equals(kind(stack));
        tooltip.add(Text.literal(gold ? "Золото" : "Деньги")
                .formatted(gold ? Formatting.GOLD : Formatting.GREEN));
        tooltip.add(Text.literal("Носится только в левой руке (спец-слот)").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("ПКМ — бросить").formatted(Formatting.GRAY));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack, true);

        ItemEntity bag = new ItemEntity(world, user.getX(), user.getEyeY() - 0.3, user.getZ(), stack.copy());
        bag.setVelocity(user.getRotationVector().multiply(0.6));
        bag.setPickupDelay(40);
        world.spawnEntity(bag);
        user.setStackInHand(hand, ItemStack.EMPTY);
        return TypedActionResult.success(ItemStack.EMPTY, false);
    }
}
