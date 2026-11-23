package heist;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Компьютер охраны: ПКМ открывает список камер в радиусе 96 блоков. */
public class SecurityComputerBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final int RANGE = 96;

    public SecurityComputerBlock(Settings settings) {
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
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        ServerWorld sw = (ServerWorld) world;
        List<BlockPos> cams = CameraRegistry.near(sw, pos, RANGE);
        if (cams.isEmpty()) {
            player.sendMessage(Text.literal("Камер в радиусе " + RANGE + " блоков не найдено."), true);
            return ActionResult.CONSUME;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(cams.size());
        for (BlockPos p : cams) {
            buf.writeBlockPos(p);
            buf.writeFloat(sw.getBlockState(p).get(SecurityCameraBlock.FACING).asRotation());
        }
        ServerPlayNetworking.send((ServerPlayerEntity) player, ModNetworking.OPEN_COMPUTER, buf);
        return ActionResult.CONSUME;
    }
}
