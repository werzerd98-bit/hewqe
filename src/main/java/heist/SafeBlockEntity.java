package heist;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class SafeBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
    public static final int SIZE = 27;

    private DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
    private boolean locked = true;
    private String code = "0000";

    public SafeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAFE, pos, state);
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        markDirty();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        markDirty();
    }

    /** Кладёт предмет в случайный свободный слот. */
    public void addLoot(ItemStack stack, Random random) {
        for (int i = 0; i < 60; i++) {
            int slot = random.nextInt(SIZE);
            if (items.get(slot).isEmpty()) {
                items.set(slot, stack);
                markDirty();
                return;
            }
        }
        for (int i = 0; i < SIZE; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack);
                markDirty();
                return;
            }
        }
    }

    public void fillRandom(int stacks, Random random) {
        for (int i = 0; i < stacks; i++) {
            addLoot(Loot.random(random), random);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items);
        locked = !nbt.contains("Locked") || nbt.getBoolean("Locked");
        code = nbt.contains("Code") ? nbt.getString("Code") : "0000";
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putBoolean("Locked", locked);
        nbt.putString("Code", code);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.heistmod.safe");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
    }

    // ---- Inventory ----

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < SIZE; i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }
}
