package heist;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

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

    /** Случайное содержимое кассы: в основном наличные, иногда золото, украшения или карта. */
    public static List<ItemStack> cashRegister(Random r) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(ModItems.CASH_BUNDLE, 3 + r.nextInt(8)));
        if (r.nextInt(100) < 35) out.add(new ItemStack(ModItems.CASH_BUNDLE, 4 + r.nextInt(7)));
        if (r.nextInt(100) < 15) out.add(new ItemStack(ModItems.GOLD_BAR, 1));
        if (r.nextInt(100) < 12) out.add(new ItemStack(ModItems.JEWELRY, 1 + r.nextInt(2)));
        if (r.nextInt(100) < 5) out.add(new ItemStack(ModItems.KEYCARD_GREEN));
        return out;
    }

    /** Случайная строка из четырёх цифр. */
    public static String randomCode(Random r) {
        return String.format("%04d", r.nextInt(10000));
    }
}
