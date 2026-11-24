package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * Взрывчатка как в играх: лепится на стену, ПКМ взводит, 5 секунд писка, потом хлопок.
 * Это НЕ настоящий взрыв: блоки и существа вокруг не страдают. Выносит только дверь под
 * взрывчатку ({@link BreachDoorBlock}), к которой заряд прилеплен.
 */
public class C4Block extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    /** 0 — не взведён, 5..1 — секунд до взрыва. */
    public static final IntProperty FUSE = IntProperty.of("fuse", 0, 5);

    private static final VoxelShape NORTH = Block.createCuboidShape(3, 3, 13, 13, 12, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(3, 3, 0, 13, 12, 3);
    private static final VoxelShape EAST = Block.createCuboidShape(0, 3, 3, 3, 12, 13);
    private static final VoxelShape WEST = Block.createCuboidShape(13, 3, 3, 16, 12, 13);

    public C4Block(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(FUSE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, FUSE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction facing = state.get(FACING);
        BlockPos support = pos.offset(facing.getOpposite());
        return world.getBlockState(support).isSideSolidFullSquare(world, support, facing);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        if (!side.getAxis().isHorizontal()) return null;
        BlockState state = getDefaultState().with(FACING, side);
        return state.canPlaceAt(ctx.getWorld(), ctx.getBlockPos()) ? state : null;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.get(FACING).getOpposite() && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        ServerWorld sw = (ServerWorld) world;
        if (state.get(FUSE) > 0) {
            player.sendMessage(Text.literal("Заряд уже взведён!").formatted(Formatting.RED), true);
            return ActionResult.CONSUME;
        }
        sw.setBlockState(pos, state.with(FUSE, 5), Block.NOTIFY_ALL);
        sw.scheduleBlockTick(pos, this, 20);
        sw.playSound(null, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 1.0f, 1.6f);
        player.sendMessage(Text.literal("Заряд взведён. Отойди!").formatted(Formatting.RED), true);
        return ActionResult.CONSUME;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int fuse = state.get(FUSE);
        if (fuse <= 0) return;

        if (fuse > 1) {
            world.setBlockState(pos, state.with(FUSE, fuse - 1), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, 20);
            world.playSound(null, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 1.0f,
                    1.6f + (5 - fuse) * 0.1f);
            return;
        }

        // «взрыв»: только эффекты, никакого урона блокам и существам
        BlockPos target = pos.offset(state.get(FACING).getOpposite());
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        world.removeBlock(pos, false);
        world.spawnParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 4, 0.5, 0.5, 0.5, 0);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.6, 0.6, 0.6, 0.02);
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0f, 1.2f);
        BreachDoorBlock.breach(world, target);
    }
}
