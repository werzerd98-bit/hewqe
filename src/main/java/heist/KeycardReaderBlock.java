package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * Считыватель ключ-карт: небольшая панель (около четверти грани блока), крепится на бок любого блока.
 * Карта включает сигнал редстоуна, и он держится, пока карту не приложат ещё раз:
 * открытая дверь сама не закрывается. Сигнал получают блок, на котором висит панель, и соседние блоки.
 */
public class KeycardReaderBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final VoxelShape NORTH = Block.createCuboidShape(5, 5, 14.6, 11, 15, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(5, 5, 0, 11, 15, 1.4);
    private static final VoxelShape EAST = Block.createCuboidShape(0, 5, 5, 1.4, 15, 11);
    private static final VoxelShape WEST = Block.createCuboidShape(14.6, 5, 5, 16, 15, 11);

    public KeycardReaderBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
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
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return net.minecraft.util.shape.VoxelShapes.empty();
    }

    private static boolean hasSupport(BlockView world, BlockPos pos, Direction facing) {
        BlockState s = world.getBlockState(pos.offset(facing.getOpposite()));
        return !s.isAir() && s.getFluidState().isEmpty();
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return hasSupport(world, pos, state.get(FACING));
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

        var stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof KeycardItem) {
            boolean on = !state.get(POWERED);
            world.setBlockState(pos, state.with(POWERED, on), Block.NOTIFY_ALL);
            // сигнал должен дойти и до блока-опоры, и до соседей опоры
            world.updateNeighborsAlways(pos.offset(state.get(FACING).getOpposite()), this);
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
