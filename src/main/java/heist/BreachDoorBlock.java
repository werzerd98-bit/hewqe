package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/** Дверь под взрывчатку: не ломается руками и не открывается, выносится только зарядом C4 (игровым). */
public class BreachDoorBlock extends Block {
    private static final int MAX_PARTS = 64;

    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public BreachDoorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        if (pos.getY() >= world.getTopY() - 1 || !world.getBlockState(pos.up()).canReplace(ctx)) return null;
        return getDefaultState().with(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable net.minecraft.entity.LivingEntity placer, ItemStack stack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                net.minecraft.world.WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.get(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)
                && !(neighborState.isOf(this) && neighborState.get(HALF) != half)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && player.isCreative()) {
            BlockPos other = state.get(HALF) == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
            BlockState os = world.getBlockState(other);
            if (os.isOf(this)) {
                world.setBlockState(other, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
            }
        }
        super.onBreak(world, pos, state, player);
    }


    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            player.sendMessage(Text.literal("Эта дверь поддаётся только взрывчатке (C4).").formatted(Formatting.GOLD), true);
        }
        return ActionResult.SUCCESS;
    }

    /** Выносит всю дверь: все соединённые секции того же вида (до 64). */
    public static int breach(ServerWorld world, BlockPos start) {
        BlockState first = world.getBlockState(start);
        if (!(first.getBlock() instanceof BreachDoorBlock)) return 0;
        Block block = first.getBlock();

        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        found.add(start.toImmutable());
        queue.add(start.toImmutable());
        while (!queue.isEmpty() && found.size() < MAX_PARTS) {
            BlockPos cur = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = cur.offset(d);
                if (found.contains(n)) continue;
                if (world.getBlockState(n).isOf(block)) {
                    found.add(n.toImmutable());
                    queue.add(n.toImmutable());
                }
            }
        }
        AlarmBlock.triggerNear(world, start, 32);
        for (BlockPos p : found) {
            world.breakBlock(p, false);
        }
        return found.size();
    }
}
