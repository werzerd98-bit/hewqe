package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/** Дверь под взрывчатку: не ломается руками и не открывается, выносится только зарядом C4 (игровым). */
public class BreachDoorBlock extends Block {
    private static final int MAX_PARTS = 64;

    public BreachDoorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            player.sendMessage(Text.literal("Эта дверь поддаётся только взрывчатке (C4).").formatted(Formatting.GOLD), true);
        }
        return ActionResult.SUCCESS;
    }

    /** Выносит всю дверь: все соединённые секции того же вида (до 64). */
    public static int breach(ServerWorld world, BlockPos start) {
        BlockState first = world.getBlockState(start);
        if (!(first.getBlock() instanceof BreachDoorBlock)) return 0;
        Block block = first.getBlock();

        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        found.add(start.toImmutable());
        queue.add(start.toImmutable());
        while (!queue.isEmpty() && found.size() < MAX_PARTS) {
            BlockPos cur = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = cur.offset(d);
                if (found.contains(n)) continue;
                if (world.getBlockState(n).isOf(block)) {
                    found.add(n.toImmutable());
                    queue.add(n.toImmutable());
                }
            }
        }
        for (BlockPos p : found) {
            world.breakBlock(p, false);
        }
        return found.size();
    }
}
