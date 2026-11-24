package heist;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Взлом замка. Игра идёт на сервере: у каждого штифта своя зелёная зона, по полосе ходит маркер
 * (его положение — функция времени, клиент рисует то же самое). Нажми вовремя, когда маркер в зоне.
 * Промах сбрасывает все штифты; три промаха ломают отмычку.
 */
public final class LockpickManager {
    private LockpickManager() {}

    public static final int MAX_FAILS = 3;
    private static final int LEAD_TICKS = 14;
    private static final int LATENCY_TICKS = 4;

    // коды ответа клиенту
    public static final int CODE_PIN = 0;
    public static final int CODE_FAIL = 1;
    public static final int CODE_OPENED = 2;
    public static final int CODE_BROKE = 3;
    public static final int CODE_CANCELLED = 4;

    private static final class Session {
        RegistryKey<World> dim;
        BlockPos pos;
        int pins;
        int halfPeriod;
        float[] center;
        float[] half;
        int index;
        long startTick;
        int fails;
        long createdTick;
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(LockpickManager::cleanup);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
    }

    public static boolean holdsLockpick(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof LockpickItem
                || player.getOffHandStack().getItem() instanceof LockpickItem;
    }

    /** Положение маркера (0..1) через ticks тиков после начала штифта. */
    public static float marker(long ticks, int halfPeriod) {
        if (ticks < 0) return 0.0f;
        float phase = (ticks % (2L * halfPeriod)) / (float) halfPeriod;
        return phase <= 1.0f ? phase : 2.0f - phase;
    }

    public static void start(ServerPlayerEntity player, ServerWorld world, BlockPos pos, int pins) {
        Random r = world.getRandom();
        Session s = new Session();
        s.dim = world.getRegistryKey();
        s.pos = pos.toImmutable();
        s.pins = pins;
        s.halfPeriod = Math.max(12, 28 - 3 * pins);
        s.center = new float[pins];
        s.half = new float[pins];
        float width = Math.max(0.06f, 0.14f - 0.015f * pins);
        for (int i = 0; i < pins; i++) {
            s.center[i] = 0.2f + r.nextFloat() * 0.6f;
            s.half[i] = width;
        }
        s.index = 0;
        s.startTick = world.getTime() + LEAD_TICKS;
        s.createdTick = world.getTime();
        SESSIONS.put(player.getUuid(), s);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeVarInt(pins);
        buf.writeVarInt(s.halfPeriod);
        buf.writeVarInt(MAX_FAILS);
        for (int i = 0; i < pins; i++) {
            buf.writeFloat(s.center[i]);
            buf.writeFloat(s.half[i]);
        }
        buf.writeLong(s.startTick);
        ServerPlayNetworking.send(player, ModNetworking.OPEN_LOCKPICK, buf);
        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.8f, 1.4f);
    }

    private static void sendState(ServerPlayerEntity player, BlockPos pos, int code, int index, long startTick, int fails) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeByte(code);
        buf.writeVarInt(index);
        buf.writeLong(startTick);
        buf.writeVarInt(fails);
        ServerPlayNetworking.send(player, ModNetworking.LOCKPICK_STATE, buf);
    }

    public static void cancel(ServerPlayerEntity player) {
        SESSIONS.remove(player.getUuid());
    }

    public static void press(ServerPlayerEntity player) {
        Session s = SESSIONS.get(player.getUuid());
        if (s == null) return;

        ServerWorld world = player.getServerWorld();
        if (!world.getRegistryKey().equals(s.dim)) {
            SESSIONS.remove(player.getUuid());
            return;
        }

        long now = world.getTime();
        if (now < s.startTick - LATENCY_TICKS) return;

        boolean hit = false;
        for (int d = 0; d <= LATENCY_TICKS; d++) {
            long t = now - d - s.startTick;
            if (t < 0) continue;
            if (Math.abs(marker(t, s.halfPeriod) - s.center[s.index]) <= s.half[s.index]) {
                hit = true;
                break;
            }
        }

        if (hit) {
            s.index++;
            world.playSound(null, s.pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.8f, 1.5f);
            if (s.index >= s.pins) {
                success(world, player, s);
            } else {
                s.startTick = now + 10;
                sendState(player, s.pos, CODE_PIN, s.index, s.startTick, s.fails);
            }
            return;
        }

        s.fails++;
        s.index = 0;
        s.startTick = now + 10;
        world.playSound(null, s.pos, SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.BLOCKS, 0.9f, 0.8f);
        if (s.fails >= MAX_FAILS) {
            breakPick(player);
            sendState(player, s.pos, CODE_BROKE, 0, 0, s.fails);
            player.sendMessage(Text.literal("Отмычка сломалась!").formatted(Formatting.RED), true);
            SESSIONS.remove(player.getUuid());
        } else {
            sendState(player, s.pos, CODE_FAIL, 0, s.startTick, s.fails);
        }
    }

    private static void breakPick(ServerPlayerEntity player) {
        if (player.isCreative()) return;
        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof LockpickItem) {
                stack.decrement(1);
                return;
            }
        }
    }

    private static void success(ServerWorld world, ServerPlayerEntity player, Session s) {
        BlockState state = world.getBlockState(s.pos);
        if (state.getBlock() instanceof PickDoorBlock) {
            PickDoorBlock.unlock(world, s.pos);
        } else if (state.getBlock() instanceof SafeBlock && world.getBlockEntity(s.pos) instanceof SafeBlockEntity safe) {
            safe.setLocked(false);
            player.openHandledScreen(safe);
        }
        world.playSound(null, s.pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.2f, 1.3f);
        player.sendMessage(Text.literal("Замок вскрыт!").formatted(Formatting.GREEN), true);
        sendState(player, s.pos, CODE_OPENED, s.pins, 0, s.fails);
        SESSIONS.remove(player.getUuid());
    }

    private static void cleanup(MinecraftServer server) {
        if (SESSIONS.isEmpty() || server.getTicks() % 20 != 0) return;

        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> e = it.next();
            Session s = e.getValue();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            boolean drop = p == null
                    || !p.getServerWorld().getRegistryKey().equals(s.dim)
                    || p.squaredDistanceTo(Vec3d.ofCenter(s.pos)) > 36.0
                    || p.getServerWorld().getTime() - s.createdTick > 20 * 120;
            if (drop) {
                if (p != null) sendState(p, s.pos, CODE_CANCELLED, 0, 0, s.fails);
                it.remove();
            }
        }
    }
}
