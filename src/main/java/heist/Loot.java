package heist;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

public final class Loot {
    private Loot() {}

    /** Случайная добыча для сейфов: каждый слот — свой бросок. */
    public static ItemStack random(Random r) {
        int roll = r.nextInt(100);
        if (roll < 30) return new ItemStack(ModItems.CASH_BUNDLE, 4 + r.nextInt(13));
        if (roll < 52) return new ItemStack(ModItems.GOLD_BAR, 1 + r.nextInt(3));
        if (roll < 70) return new ItemStack(ModItems.JEWELRY, 1 + r.nextInt(3));
        if (roll < 80) return new ItemStack(ModBlocks.PAINTING);
        if (roll < 90) return new ItemStack(ModBlocks.MONEY_BLOCK, 1 + r.nextInt(2));
        return new ItemStack(ModBlocks.GOLD_PILE);
    }

    /** Случайная строка из четырёх цифр. */
    public static String randomCode(Random r) {
        return String.format("%04d", r.nextInt(10000));
    }
}
