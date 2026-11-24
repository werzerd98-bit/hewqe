package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Сейф с золотом или деньгами. Закрытый открывается только сверлением (дрелью или термобуром).
 * В открытом лежит добыча: каждое ПКМ перекладывает единицу в сумку в левой руке (всего 4 единицы).
 */
public class LootSafeBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;
    public static final IntProperty UNITS = IntProperty.of("units", 0, 4);

    private final String kind;

    public LootSafeBlock(Settings settings, String kind) {
        super(settings);
        this.kind = kind;
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH).with(OPEN, false).with(UNITS, 4));
    }

    public String getKind() {
        return kind;
    }

    /** Золотой сейф сверлится в полтора раза дольше. */
    public double drillFactor() {
        return LootBagItem.KIND_GOLD.equals(kind) ? 1.5 : 1.0;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, UNITS);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    /** Вызывается, когда сверление закончилось. */
    public static void open(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(OPEN, true), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 0.7f);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        ServerWorld sw = (ServerWorld) world;

        if (!state.get(OPEN)) {
            if (DrillManager.isActive(sw, pos)) {
                player.sendMessage(Text.literal("Сейф сверлят... подожди."), true);
            } else {
                player.sendMessage(Text.literal("Сейф заперт. Просверли его: поставь дрель или термобур вплотную."), true);
            }
            return ActionResult.CONSUME;
        }

        int units = state.get(UNITS);
        if (units <= 0) {
            player.sendMessage(Text.literal("Сейф пуст."), true);
        } else if (LootBags.tryCollect(player, kind)) {
            world.setBlockState(pos, state.with(UNITS, units - 1), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.7f, 0.8f);
            player.sendMessage(Text.literal("Добыча в сумке: " + LootBagItem.units(player.getOffHandStack())
                    + " / " + LootBagItem.CAPACITY).formatted(Formatting.GREEN), true);
        } else {
            player.sendMessage(Text.literal("Левая рука занята или сумка полна.").formatted(Formatting.RED), true);
        }
        return ActionResult.CONSUME;
    }
}
