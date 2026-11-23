package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Дверь хранилища: вскрывается только дрелью (бурение 60 секунд). */
public class VaultDoorBlock extends Block {
    public VaultDoorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        ServerWorld sw = (ServerWorld) world;
        if (DrillManager.isJammed(sw, pos)) {
            DrillManager.fix(sw, pos, player);
        } else if (DrillManager.isActive(sw, pos)) {
            player.sendMessage(Text.literal("Дверь сверлится... держи оборону!"), true);
        } else {
            player.sendMessage(Text.literal("Дверь хранилища. Поставь бур вплотную к двери."), true);
        }
        return ActionResult.CONSUME;
    }
}
