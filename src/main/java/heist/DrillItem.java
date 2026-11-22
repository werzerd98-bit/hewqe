package heist;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DrillItem extends net.minecraft.item.Item {
    public DrillItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!DrillManager.isDrillable(state)) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;

        PlayerEntity player = ctx.getPlayer();
        ServerWorld sw = (ServerWorld) world;
        if (player == null) return ActionResult.PASS;

        if (DrillManager.isActive(sw, pos)) {
            player.sendMessage(Text.literal("Здесь уже сверлят."), true);
            return ActionResult.CONSUME;
        }
        if (world.getBlockEntity(pos) instanceof SafeBlockEntity safe && !safe.isLocked()) {
            player.sendMessage(Text.literal("Сейф уже открыт."), true);
            return ActionResult.CONSUME;
        }

        DrillManager.start(sw, pos);
        player.sendMessage(Text.literal("Дрель установлена. Не отходи далеко — она может заклинить!"), true);
        if (!player.getAbilities().creativeMode) {
            ctx.getStack().damage(1, player, p -> p.sendToolBreakStatus(ctx.getHand()));
        }
        return ActionResult.CONSUME;
    }
}
