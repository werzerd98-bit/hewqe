package heist;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Кодовая панель: верный код на 5 секунд подаёт сигнал редстоуна (открывает большие двери). */
public class CodePanelBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final VoxelShape NORTH = Block.createCuboidShape(5, 5, 14.6, 11, 15, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(5, 5, 0, 11, 15, 1.4);
    private static final VoxelShape EAST = Block.createCuboidShape(0, 5, 5, 1.4, 15, 11);
    private static final VoxelShape WEST = Block.createCuboidShape(14.6, 5, 5, 16, 15, 11);

    public CodePanelBlock(Settings settings) {
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
        return VoxelShapes.empty();
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState s = world.getBlockState(pos.offset(state.get(FACING).getOpposite()));
        return !s.isAir() && s.getFluidState().isEmpty();
    }

    /** Панель вешается на бок блока, как картина. */
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
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CodePanelBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient || !(world.getBlockEntity(pos) instanceof CodePanelBlockEntity panel)) return;
        panel.setCode(Loot.randomCode(world.getRandom()));
        if (placer instanceof ServerPlayerEntity sp && sp.hasPermissionLevel(2)) {
            sp.sendMessage(Text.literal("Кодовая панель. Код: " + panel.getCode()), true);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (state.get(POWERED)) return ActionResult.CONSUME;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ServerPlayNetworking.send((ServerPlayerEntity) player, ModNetworking.OPEN_KEYPAD, buf);
        return ActionResult.CONSUME;
    }

    /** Вызывается из сетевого обработчика, когда игрок ввёл код. */
    public static void tryUnlock(ServerWorld world, BlockPos pos, ServerPlayerEntity player, CodePanelBlockEntity panel, String code) {
        if (!panel.getCode().equals(code)) {
            world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 1.0f, 0.8f);
            player.sendMessage(Text.literal("Неверный код!").formatted(Formatting.RED), true);
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CodePanelBlock) || state.get(POWERED)) return;

        world.setBlockState(pos, state.with(POWERED, true), Block.NOTIFY_ALL);
        world.updateNeighborsAlways(pos.offset(state.get(FACING).getOpposite()), state.getBlock());
        world.scheduleBlockTick(pos, state.getBlock(), 100);
        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.5f, 1.4f);
        player.sendMessage(Text.literal("Код верный").formatted(Formatting.GREEN), true);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, false), Block.NOTIFY_ALL);
            world.updateNeighborsAlways(pos.offset(state.get(FACING).getOpposite()), state.getBlock());
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
