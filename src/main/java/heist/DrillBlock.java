package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Бур в виде блока. Ставится вплотную к сейфу или двери хранилища и сам начинает сверлить.
 * Если заклинило — ПКМ по буру. Сломал бур — бурение остановилось.
 */
public class DrillBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public DrillBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        Direction facing = side.getAxis().isHorizontal() ? side : ctx.getHorizontalPlayerFacing().getOpposite();
        return getDefaultState().with(FACING, facing);
    }

    @Nullable
    public static BlockPos findTarget(World world, BlockPos pos, Direction preferred) {
        BlockPos first = pos.offset(preferred);
        if (DrillManager.isDrillable(world.getBlockState(first))) return first;
        for (Direction d : Direction.values()) {
            BlockPos p = pos.offset(d);
            if (DrillManager.isDrillable(world.getBlockState(p))) return p;
        }
        return null;
    }

    private static void tryStart(ServerWorld world, BlockPos target, BlockPos drill, @Nullable PlayerEntity player) {
        if (world.getBlockEntity(target) instanceof SafeBlockEntity safe && !safe.isLocked()) {
            if (player != null) player.sendMessage(Text.literal("Сейф уже открыт."), true);
            return;
        }
        DrillManager.start(world, target, drill);
        if (player != null) {
            player.sendMessage(Text.literal("Бур запущен. Он может заклинить — тогда ПКМ по буру!").formatted(Formatting.YELLOW), true);
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient) return;

        ServerWorld sw = (ServerWorld) world;
        PlayerEntity player = placer instanceof PlayerEntity p ? p : null;
        BlockPos target = findTarget(world, pos, state.get(FACING).getOpposite());
        if (target == null) {
            if (player != null) {
                player.sendMessage(Text.literal("Поставь бур вплотную к сейфу или двери хранилища."), true);
            }
            return;
        }
        if (!DrillManager.isActive(sw, target)) {
            tryStart(sw, target, pos, player);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        ServerWorld sw = (ServerWorld) world;
        BlockPos target = findTarget(world, pos, state.get(FACING).getOpposite());
        if (target == null) {
            player.sendMessage(Text.literal("Рядом нет сейфа или двери хранилища."), true);
        } else if (DrillManager.isJammed(sw, target)) {
            DrillManager.fix(sw, target, player);
        } else if (DrillManager.isActive(sw, target)) {
            player.sendMessage(Text.literal("Бур работает..."), true);
        } else {
            tryStart(sw, target, pos, player);
        }
        return ActionResult.CONSUME;
    }
}
