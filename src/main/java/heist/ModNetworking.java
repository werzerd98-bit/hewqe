package heist;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class ModNetworking {
    private ModNetworking() {}

    /** Сервер -> клиент: открыть экран кодовой клавиатуры (BlockPos). */
    public static final Identifier OPEN_KEYPAD = HeistMod.id("open_keypad");
    /** Сервер -> клиент: список камер (VarInt n, затем n раз BlockPos + float yaw). */
    public static final Identifier OPEN_COMPUTER = HeistMod.id("open_computer");
    /** Клиент -> сервер: отправить введённый код (BlockPos, String). */
    public static final Identifier SUBMIT_CODE = HeistMod.id("submit_code");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SUBMIT_CODE, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            String code = buf.readString(8);
            server.execute(() -> handleCode(player, pos, code));
        });
    }

    private static void handleCode(ServerPlayerEntity player, BlockPos pos, String code) {
        ServerWorld world = player.getServerWorld();
        if (player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 64.0) return;
        if (world.getBlockEntity(pos) instanceof CodePanelBlockEntity panel) {
            CodePanelBlock.tryUnlock(world, pos, player, panel, code);
            return;
        }
        if (!(world.getBlockEntity(pos) instanceof SafeBlockEntity safe)) return;

        if (!safe.isLocked()) {
            player.openHandledScreen(safe);
            return;
        }
        if (safe.getCode().equals(code)) {
            safe.setLocked(false);
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 1.2f);
            player.sendMessage(Text.literal("Код верный. Сейф открыт.").formatted(Formatting.GREEN), true);
            player.openHandledScreen(safe);
        } else {
            world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 1.0f, 0.8f);
            player.sendMessage(Text.literal("Неверный код!").formatted(Formatting.RED), true);
        }
    }
}
