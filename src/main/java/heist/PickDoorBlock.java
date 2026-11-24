package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
 * образуют одну дверь любого размера. Открытую дверь можно закрыть ПКМ (она снова будет заперта).
 */
public class PickDoorBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;

    private static final int MAX_PARTS = 64;

    private final int pins;

    public PickDoorBlock(Settings settings, int pins) {
        super(settings);
        this.pins = pins;
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
    }

    public int getPins() {
        return pins;
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
            lock(sw, pos, this, state.get(FACING).getAxis());
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
