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
    /** Сервер -> клиент: открыть окно бура (BlockPos). */
    public static final Identifier OPEN_DRILL = HeistMod.id("open_drill");
    /** Сервер -> клиент: состояние бура (BlockPos, byte state, int left, int total). */
    public static final Identifier DRILL_STATUS = HeistMod.id("drill_status");
    /** Клиент -> сервер: действие в окне бура (BlockPos, byte: 0 запустить, 1 починить). */
    public static final Identifier DRILL_ACTION = HeistMod.id("drill_action");
    /** Клиент -> сервер: отправить введённый код (BlockPos, String). */
    public static final Identifier SUBMIT_CODE = HeistMod.id("submit_code");

    /** Сервер -> клиент: начало взлома (pos, pins, halfPeriod, maxFails, pins*(center,half), startTick). */
    public static final Identifier OPEN_LOCKPICK = HeistMod.id("open_lockpick");
    /** Сервер -> клиент: ход взлома (pos, byte code, index, startTick, fails). */
    public static final Identifier LOCKPICK_STATE = HeistMod.id("lockpick_state");
    /** Клиент -> сервер: действие взлома (byte: 0 нажать, 1 отменить). */
    public static final Identifier LOCKPICK_ACTION = HeistMod.id("lockpick_action");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(LOCKPICK_ACTION, (server, player, handler, buf, responseSender) -> {
            int action = buf.readByte();
            server.execute(() -> {
                if (action == 0) LockpickManager.press(player);
                else LockpickManager.cancel(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DRILL_ACTION, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int action = buf.readByte();
            server.execute(() -> {
                if (player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 100.0) return;
                DrillBlock.handleAction(player.getServerWorld(), pos, player, action);
            });
        });

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
