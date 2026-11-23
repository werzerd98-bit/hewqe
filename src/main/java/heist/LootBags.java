package heist;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Правила переноски сумок: одна сумка, только в левой руке; с сумкой игрок замедляется
 * (полная сумка — сильнее). Сумка в обычном инвентаре переезжает в левую руку или выбрасывается.
 */
public final class LootBags {
    private LootBags() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 5 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                tickPlayer(p);
            }
        });
    }

    /** Кладёт единицу добычи в сумку в левой руке. false — если руки заняты или сумка не подходит. */
    public static boolean tryCollect(PlayerEntity player, String kind) {
        ItemStack off = player.getOffHandStack();
        if (off.isEmpty()) {
            player.setStackInHand(Hand.OFF_HAND, LootBagItem.create(kind, 1));
            return true;
        }
        if (off.getItem() instanceof LootBagItem
                && kind.equals(LootBagItem.kind(off))
                && LootBagItem.units(off) < LootBagItem.CAPACITY) {
            off.getOrCreateNbt().putInt("Units", LootBagItem.units(off) + 1);
            return true;
        }
        return false;
    }

    private static void tickPlayer(ServerPlayerEntity p) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack s = inv.main.get(i);
            if (!(s.getItem() instanceof LootBagItem)) continue;

            if (p.getOffHandStack().isEmpty()) {
                p.setStackInHand(Hand.OFF_HAND, s.copy());
                inv.main.set(i, ItemStack.EMPTY);
                p.sendMessage(Text.literal("Сумка перенесена в левую руку (спец-слот).").formatted(Formatting.YELLOW), true);
            } else {
                ItemEntity e = new ItemEntity(p.getWorld(), p.getX(), p.getEyeY() - 0.3, p.getZ(), s.copy());
                e.setVelocity(p.getRotationVector().multiply(0.4));
                e.setPickupDelay(60);
                p.getWorld().spawnEntity(e);
                inv.main.set(i, ItemStack.EMPTY);
                p.sendMessage(Text.literal("Сумку можно нести только в левой руке — освободи её.").formatted(Formatting.RED), true);
            }
        }

        ItemStack off = p.getOffHandStack();
        if (off.getItem() instanceof LootBagItem) {
            int amp = LootBagItem.units(off) >= LootBagItem.CAPACITY ? 1 : 0;
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, amp, false, false, false));
        }
    }
}
