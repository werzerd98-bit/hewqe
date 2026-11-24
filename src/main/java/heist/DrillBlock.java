package heist;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
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
 * Бур в виде блока: дрель или термобур (см. DrillType). Ставится вплотную к сейфу или двери
 * хранилища и сам начинает сверлить. ПКМ открывает окно со временем до конца, кнопки
 * «Запустить» и «Починить». Сломал бур — бурение остановилось.
 */
public class DrillBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private final DrillType type;

    public DrillBlock(Settings settings, DrillType type) {
        super(settings);
        this.type = type;
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    public DrillType getType() {
        return type;
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

    private void startAt(ServerWorld world, BlockPos target, BlockPos drill, @Nullable PlayerEntity player) {
        BlockState targetState = world.getBlockState(target);
        if (targetState.getBlock() instanceof VaultDoorBlock && type.vaultTicks <= 0) {
            if (player != null) {
                player.sendMessage(Text.literal("Дрелью дверь хранилища не взять — нужен термобур.").formatted(Formatting.RED), true);
            }
            return;
        }
        if (world.getBlockEntity(target) instanceof SafeBlockEntity safe && !safe.isLocked()) {
            if (player != null) player.sendMessage(Text.literal("Сейф уже открыт."), true);
            return;
        }
        DrillManager.start(world, target, drill);
        if (player != null) {
            player.sendMessage(Text.literal("Бур запущен. Он может заклинить — тогда нажми «Починить» в окне бура.")
                    .formatted(Formatting.YELLOW), true);
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
            startAt(sw, target, pos, player);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        ServerWorld sw = (ServerWorld) world;
        ServerPlayerEntity sp = (ServerPlayerEntity) player;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ServerPlayNetworking.send(sp, ModNetworking.OPEN_DRILL, buf);
        DrillManager.sendStatusFor(sw, sp, pos);
        return ActionResult.CONSUME;
    }

    /** Действие из окна бура: 0 — запустить, 1 — починить. */
    public static void handleAction(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int action) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof DrillBlock drill)) return;

        BlockPos target = findTarget(world, pos, state.get(FACING).getOpposite());
        if (target == null) {
            player.sendMessage(Text.literal("Рядом нет сейфа или двери хранилища."), true);
        } else if (action == 1) {
            if (DrillManager.isJammed(world, target)) DrillManager.fix(world, target, player);
        } else if (action == 0 && !DrillManager.isActive(world, target)) {
            drill.startAt(world, target, pos, player);
        }
        DrillManager.sendStatusFor(world, player, pos);
    }
}
