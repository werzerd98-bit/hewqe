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
 * Считыватель ключ-карт. Если приложить карту нужного уровня (или выше),
 * блок на 5 секунд выдаёт сигнал редстоуна — рядом ставится железная дверь.
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
        if (stack.getItem() instanceof KeycardItem card && card.getTier() >= tier) {
            if (!state.get(POWERED)) {
                world.setBlockState(pos, state.with(POWERED, true), Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, this, 100);
                world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.5f, 1.4f);
                player.sendMessage(Text.literal("Доступ разрешён").formatted(Formatting.GREEN), true);
            }
        } else {
            world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 0.8f, 1.0f);
            player.sendMessage(Text.literal("Доступ запрещён. Нужна ключ-карта уровня " + tier).formatted(Formatting.RED), true);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, false), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 0.6f, 0.8f);
        }
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
