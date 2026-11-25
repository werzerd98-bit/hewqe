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
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * Сигнализация: настенная панель. Срабатывает на сигнал редстоуна, а также сама, когда рядом
 * (до 32 блоков) начинают сверлить или выносят дверь взрывчаткой. 20 секунд воет сирена,
 * ПКМ выключает.
 */
public class AlarmBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    /** Сколько секунд сирена ещё будет выть (0 — выключена). */
    public static final IntProperty LEFT = IntProperty.of("left", 0, 20);

    private static final VoxelShape EAST = Block.createCuboidShape(0, 3, 1, 1, 13, 15);
    private static final VoxelShape WEST = Block.createCuboidShape(15, 3, 1, 16, 13, 15);
    private static final VoxelShape NORTH = Block.createCuboidShape(1, 3, 15, 15, 13, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(1, 3, 0, 15, 13, 1);

    public AlarmBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.EAST).with(LEFT, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case WEST -> WEST;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            default -> EAST;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState s = world.getBlockState(pos.offset(state.get(FACING).getOpposite()));
        return !s.isAir() && s.getFluidState().isEmpty();
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
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient && state.get(LEFT) == 0 && world.isReceivingRedstonePower(pos)) {
            trigger((ServerWorld) world, pos);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (state.get(LEFT) > 0) {
            world.setBlockState(pos, state.with(LEFT, 0), Block.NOTIFY_ALL);
            player.sendMessage(Text.literal("Сигнализация выключена").formatted(Formatting.YELLOW), true);
        } else {
            trigger((ServerWorld) world, pos);
        }
        return ActionResult.CONSUME;
    }

    /** Включает эту сигнализацию. */
    public static void trigger(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof AlarmBlock) || state.get(LEFT) > 0) return;
        world.setBlockState(pos, state.with(LEFT, 20), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, state.getBlock(), 1);
    }

    /** Включает все сигнализации в радиусе radius блоков. */
    public static void triggerNear(ServerWorld world, BlockPos center, int radius) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius / 2; dy <= radius / 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (world.getBlockState(m).getBlock() instanceof AlarmBlock) {
                        trigger(world, m.toImmutable());
                    }
                }
            }
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int left = state.get(LEFT);
        if (left <= 0) return;

        world.playSound(null, pos, SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 3.0f, 1.9f);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 1.0f, 2.0f);
        world.spawnParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 1.0);
        if (left == 20) {
            for (PlayerEntity p : world.getPlayers()) {
                if (p.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 48 * 48) {
                    p.sendMessage(Text.literal("ТРЕВОГА!").formatted(Formatting.RED, Formatting.BOLD), true);
                }
            }
        }
        world.setBlockState(pos, state.with(LEFT, left - 1), Block.NOTIFY_ALL);
        if (left - 1 > 0) {
            world.scheduleBlockTick(pos, this, 20);
        }
    }
}
