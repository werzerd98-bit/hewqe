package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/** Фургон: принимает сумки (брошенные рядом или поднесённые ПКМ) и платит изумрудами. */
public class EscapeVanBlockEntity extends BlockEntity {
    public static final int EMERALDS_PER_MONEY_UNIT = 3;
    public static final int EMERALDS_PER_GOLD_UNIT = 6;

    private int moneyTotal;
    private int goldTotal;

    public EscapeVanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VAN, pos, state);
    }

    public int getMoneyTotal() {
        return moneyTotal;
    }

    public int getGoldTotal() {
        return goldTotal;
    }

    public void deliver(ServerWorld world, BlockPos pos, ItemStack bag) {
        int units = LootBagItem.units(bag);
        if (units <= 0) return;

        boolean gold = LootBagItem.KIND_GOLD.equals(LootBagItem.kind(bag));
        if (gold) goldTotal += units; else moneyTotal += units;
        markDirty();

        int left = units * (gold ? EMERALDS_PER_GOLD_UNIT : EMERALDS_PER_MONEY_UNIT);
        int reward = left;
        while (left > 0) {
            int n = Math.min(64, left);
            Block.dropStack(world, pos.up(), new ItemStack(Items.EMERALD, n));
            left -= n;
        }

        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.2f);
        Text msg = Text.literal("Сумка сдана (" + (gold ? "золото" : "деньги") + " ×" + units + "): +" + reward
                + " изумрудов. Всего: деньги " + moneyTotal + ", золото " + goldTotal)
                .formatted(Formatting.GREEN);
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 16 * 16) {
                p.sendMessage(msg, true);
            }
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, EscapeVanBlockEntity van) {
        if (world.isClient || world.getTime() % 10 != 0) return;

        Box area = new Box(pos).expand(3.0);
        for (ItemEntity e : world.getEntitiesByClass(ItemEntity.class, area, i -> i.getStack().getItem() instanceof LootBagItem)) {
            van.deliver((ServerWorld) world, pos, e.getStack());
            e.discard();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        moneyTotal = nbt.getInt("Money");
        goldTotal = nbt.getInt("Gold");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("Money", moneyTotal);
        nbt.putInt("Gold", goldTotal);
    }
}
