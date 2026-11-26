package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.EnumProperty;

import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Запертая дверь: открывается только взломом отмычкой (мини-игра). Секции в одной плоскости
 * образуют одну дверь любого размера. Вскрытая дверь остаётся открытой.
 */
public class PickDoorBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    private static final int MAX_PARTS = 64;

    private final int pins;

    public PickDoorBlock(Settings settings, int pins) {
        super(settings);
        this.pins = pins;
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false).with(HALF, DoubleBlockHalf.LOWER));
    }

    public int getPins() {
        return pins;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HALF);
    }


    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        if (pos.getY() >= world.getTopY() - 1 || !world.getBlockState(pos.up()).canReplace(ctx)) return null;
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing()).with(OPEN, false).with(HALF, DoubleBlockHalf.LOWER);
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
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(OPEN) ? VoxelShapes.empty() : VoxelShapes.fullCube();
    }

    static Set<BlockPos> collect(World world, BlockPos start, Block block, Direction.Axis axis) {
        Direction[] dirs = axis == Direction.Axis.Z
                ? new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}
                : new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};

        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        found.add(start.toImmutable());
        queue.add(start.toImmutable());
        while (!queue.isEmpty() && found.size() < MAX_PARTS) {
            BlockPos cur = queue.poll();
            for (Direction d : dirs) {
                BlockPos n = cur.offset(d);
                if (found.contains(n)) continue;
                BlockState s = world.getBlockState(n);
                if (s.isOf(block) && s.get(FACING).getAxis() == axis) {
                    found.add(n.toImmutable());
                    queue.add(n.toImmutable());
                }
            }
        }
        return found;
    }

    /** Открывает всю дверь (вызывается после успешного взлома). */
    public static void unlock(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof PickDoorBlock block)) return;
        for (BlockPos p : collect(world, pos, block, state.get(FACING).getAxis())) {
            world.setBlockState(p, world.getBlockState(p).with(OPEN, true), Block.NOTIFY_ALL);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 0.9f);
    }

    private static void lock(ServerWorld world, BlockPos pos, PickDoorBlock block, Direction.Axis axis) {
        Set<BlockPos> parts = collect(world, pos, block, axis);
        for (BlockPos p : parts) {
            if (!world.getOtherEntities((Entity) null, new Box(p), e -> !e.isSpectator()).isEmpty()) return;
        }
        for (BlockPos p : parts) {
            world.setBlockState(p, world.getBlockState(p).with(OPEN, false), Block.NOTIFY_ALL);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0f, 0.9f);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        ServerWorld sw = (ServerWorld) world;

        if (state.get(OPEN)) {
            player.sendMessage(Text.literal("Дверь вскрыта и остаётся открытой.").formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }

        if (LockpickManager.holdsLockpick(player) && player instanceof ServerPlayerEntity sp) {
            LockpickManager.start(sp, sw, pos, pins);
        } else {
            player.sendMessage(Text.literal("Заперто. Нужна отмычка (штифтов: " + pins + ").").formatted(Formatting.RED), true);
        }
        return ActionResult.CONSUME;
    }
}
