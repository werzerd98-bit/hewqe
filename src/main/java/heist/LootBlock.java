package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Блок денег или золота: при добыче добыча падает в сумку в левой руке. */
public class LootBlock extends Block {
    private final String kind;

    public LootBlock(Settings settings, String kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state,
                           @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!world.isClient) {
            if (LootBags.tryCollect(player, kind)) {
                player.incrementStat(Stats.MINED.getOrCreateStat(this));
                player.addExhaustion(0.005f);
                world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.6f, 0.9f);
                player.sendMessage(Text.literal("Добыча в сумке").formatted(Formatting.GREEN), true);
                return;
            }
            player.sendMessage(Text.literal("Левая рука занята или сумка полна — добыча выпала на землю.")
                    .formatted(Formatting.RED), true);
        }
        super.afterBreak(world, player, pos, state, blockEntity, tool);
    }
}
