package heist;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    private ModItems() {}

    /** Единая ключ-карта: открывает любой считыватель. */
    public static final Item KEYCARD = reg("keycard", new KeycardItem(new FabricItemSettings().maxCount(1)));

    public static final Item GOLD_BAR = reg("gold_bar", new Item(new FabricItemSettings().maxCount(16)));
    public static final Item CASH_BUNDLE = reg("cash_bundle", new Item(new FabricItemSettings()));
    public static final Item JEWELRY = reg("jewelry", new Item(new FabricItemSettings().maxCount(16)));

    public static final Item LOOT_BAG = reg("loot_bag", new LootBagItem(new FabricItemSettings().maxCount(1)));

    public static final Item LOCKPICK = reg("lockpick", new LockpickItem(new FabricItemSettings().maxCount(16)));

    public static final Item CODE_NOTE = reg("code_note", new CodeNoteItem(new FabricItemSettings().maxCount(1)));

    private static Item reg(String name, Item item) {
        return Registry.register(Registries.ITEM, HeistMod.id(name), item);
    }

    public static ItemStack codeNote(String code, String hint) {
        ItemStack stack = new ItemStack(CODE_NOTE);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString("Code", code);
        nbt.putString("Hint", hint);
        return stack;
    }

    public static void init() {}
}
