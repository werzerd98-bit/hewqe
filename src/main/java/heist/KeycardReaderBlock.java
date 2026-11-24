package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Считыватель ключ-карт. Карта включает сигнал редстоуна, и он держится, пока карту не приложат
 * ещё раз: открытая дверь сама не закрывается.
 */
public class KeycardReaderBlock extends Block {
    public static final BooleanProperty POWERED = Properties.POWERED;

    private final int tier;

    public KeycardReaderBlock(Settings settings, int tier) {
        super(settings);
        this.tier = tier;
        setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
    }

    public int getTier() {
        return tier;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        var stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof KeycardItem) {
            boolean on = !state.get(POWERED);
            world.setBlockState(pos, state.with(POWERED, on), Block.NOTIFY_ALL);
            if (on) {
                world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.5f, 1.4f);
                player.sendMessage(Text.literal("Доступ разрешён: дверь открыта").formatted(Formatting.GREEN), true);
            } else {
                world.playSound(null, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 0.8f, 0.8f);
                player.sendMessage(Text.literal("Дверь закрыта").formatted(Formatting.YELLOW), true);
            }
        } else {
            world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 0.8f, 1.0f);
            player.sendMessage(Text.literal("Доступ запрещён. Нужна ключ-карта.").formatted(Formatting.RED), true);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }
}
