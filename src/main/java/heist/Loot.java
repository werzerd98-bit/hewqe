package heist;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

public final class Loot {
    private Loot() {}

    /** Случайная добыча для сейфов и сундуков. */
    public static ItemStack random(Random r) {
        int roll = r.nextInt(100);
        if (roll < 40) return new ItemStack(ModItems.CASH_BUNDLE, 4 + r.nextInt(13));
        if (roll < 65) return new ItemStack(ModItems.GOLD_BAR, 1 + r.nextInt(3));
        if (roll < 85) return new ItemStack(ModItems.JEWELRY, 1 + r.nextInt(3));
        return new ItemStack(ModItems.PAINTING);
    }
}
