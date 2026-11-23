package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Секция большой двери. Все соседние секции в одной плоскости образуют одну дверь любого размера
 * (до 64 секций). Дверь открывается, когда на любую её секцию подан сигнал редстоуна
 * (считыватель ключ-карт, кодовая панель, рычаг...), и закрывается, когда сигнал пропал.
 * Секции уходят по рядам: открытие снизу вверх, закрытие сверху вниз.
 */
public class SecurityDoorBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;

    private static final int MAX_PARTS = 64;
    private static final int ROW_DELAY = 3;

    public SecurityDoorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(OPEN) ? VoxelShapes.empty() : VoxelShapes.fullCube();
    }

    /** Все секции этой двери: соседи вверх, вниз и вбок в плоскости двери. */
    private Set<BlockPos> collect(World world, BlockPos start, Direction facing) {
        Direction.Axis axis = facing.getAxis();
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
                if (s.isOf(this) && s.get(FACING).getAxis() == axis) {
                    found.add(n.toImmutable());
                    queue.add(n.toImmutable());
                }
            }
        }
        return found;
    }

    private static boolean powered(World world, Set<BlockPos> door) {
        for (BlockPos p : door) {
            if (world.isReceivingRedstonePower(p)) return true;
        }
        return false;
    }

    private void scheduleDoor(World world, BlockPos pos, BlockState state) {
        Set<BlockPos> door = collect(world, pos, state.get(FACING));
        boolean shouldOpen = powered(world, door);

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos p : door) {
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }
        for (BlockPos p : door) {
            if (world.getBlockState(p).get(OPEN) == shouldOpen) continue;
            int row = shouldOpen ? p.getY() - minY : maxY - p.getY();
            world.scheduleBlockTick(p, this, 2 + ROW_DELAY * row);
        }
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient && !oldState.isOf(this)) {
            scheduleDoor(world, pos, state);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            scheduleDoor(world, pos, state);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean shouldOpen = powered(world, collect(world, pos, state.get(FACING)));
        if (state.get(OPEN) == shouldOpen) return;

        // закрываться на голову игроку или мобу нельзя — ждём, пока проём освободится
        if (!shouldOpen && !world.getOtherEntities((Entity) null, new Box(pos), e -> !e.isSpectator()).isEmpty()) {
            world.scheduleBlockTick(pos, this, 10);
            return;
        }

        world.setBlockState(pos, state.with(OPEN, shouldOpen), Block.NOTIFY_LISTENERS);
        if (random.nextInt(3) == 0) {
            world.playSound(null, pos, shouldOpen ? SoundEvents.BLOCK_PISTON_EXTEND : SoundEvents.BLOCK_PISTON_CONTRACT,
                    SoundCategory.BLOCKS, 0.7f, 0.7f);
        }
    }
}
