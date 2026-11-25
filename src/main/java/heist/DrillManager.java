package heist;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Следит за всеми дрелями, которые сейчас сверлят сейфы и двери хранилищ. */
public final class DrillManager {
    private DrillManager() {}

    private record Key(RegistryKey<World> dim, BlockPos pos) {}

    private static final class Session {
        final int total;
        int ticks = 0;
        boolean jammed = false;

        final BlockPos drill;
        final int jamOdds;

        Session(int total, BlockPos drill, int jamOdds) {
            this.total = total;
            this.drill = drill;
            this.jamOdds = jamOdds;
        }
    }

    private static final Map<Key, Session> SESSIONS = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(DrillManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
    }

    public static boolean isDrillable(BlockState state) {
        if (state.getBlock() instanceof LootSafeBlock) return !state.get(LootSafeBlock.OPEN);
        return state.getBlock() instanceof SafeBlock || state.getBlock() instanceof VaultDoorBlock;
    }

    private static Key key(ServerWorld world, BlockPos pos) {
        return new Key(world.getRegistryKey(), pos.toImmutable());
    }

    public static boolean isActive(ServerWorld world, BlockPos pos) {
        return SESSIONS.containsKey(key(world, pos));
    }

    public static boolean isJammed(ServerWorld world, BlockPos pos) {
        Session s = SESSIONS.get(key(world, pos));
        return s != null && s.jammed;
    }

    public static void fix(ServerWorld world, BlockPos pos, PlayerEntity player) {
        Session s = SESSIONS.get(key(world, pos));
        if (s == null) return;
        s.jammed = false;
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.7f, 1.5f);
        player.sendMessage(Text.literal("Бур починен!").formatted(Formatting.GREEN), true);
        if (s.drill != null) broadcastStatus(world, s.drill, 1, s.total - s.ticks, s.total);
    }

    public static void start(ServerWorld world, BlockPos pos) {
        start(world, pos, null);
    }

    public static void start(ServerWorld world, BlockPos pos, BlockPos drill) {
        BlockState state = world.getBlockState(pos);
        boolean vault = state.getBlock() instanceof VaultDoorBlock;
        DrillType type = drill != null && world.getBlockState(drill).getBlock() instanceof DrillBlock db
                ? db.getType() : DrillType.THERMAL;
        double factor = state.getBlock() instanceof LootSafeBlock ls ? ls.drillFactor() : 1.0;
        int total = (int) ((vault ? type.vaultTicks : type.safeTicks) * factor);
        Session session = new Session(Math.max(total, 20), drill == null ? null : drill.toImmutable(), type.jamOdds);
        SESSIONS.put(key(world, pos), session);
        if (drill != null) broadcastStatus(world, drill, 1, session.total, session.total);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        AlarmBlock.triggerNear(world, pos, 32);
        if (vault) {
            alarm(world, pos);
        }
    }

    /** Состояние для окна бура: 0 — не работает, 1 — работает, 2 — заклинило, 3 — готово. */
    public static void sendStatus(ServerPlayerEntity player, BlockPos drill, int state, int left, int total) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(drill);
        buf.writeByte(state);
        buf.writeInt(left);
        buf.writeInt(total);
        ServerPlayNetworking.send(player, ModNetworking.DRILL_STATUS, buf);
    }

    private static void broadcastStatus(ServerWorld world, BlockPos drill, int state, int left, int total) {
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(drill.getX() + 0.5, drill.getY() + 0.5, drill.getZ() + 0.5) < 12 * 12) {
                sendStatus(p, drill, state, Math.max(left, 0), total);
            }
        }
    }

    /** Отправляет игроку текущее состояние конкретного бура (когда он открывает окно). */
    public static void sendStatusFor(ServerWorld world, ServerPlayerEntity player, BlockPos drill) {
        for (Map.Entry<Key, Session> e : SESSIONS.entrySet()) {
            Session s = e.getValue();
            if (e.getKey().dim().equals(world.getRegistryKey()) && drill.equals(s.drill)) {
                sendStatus(player, drill, s.jammed ? 2 : 1, Math.max(s.total - s.ticks, 0), s.total);
                return;
            }
        }
        sendStatus(player, drill, 0, 0, 0);
    }

    private static void alarm(ServerWorld world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 4.0f, 0.5f);
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 48 * 48) {
                p.sendMessage(Text.literal("ТРЕВОГА! Кто-то сверлит дверь хранилища!")
                        .formatted(Formatting.RED, Formatting.BOLD), false);
            }
        }
    }

    private static void tick(ServerWorld world) {
        if (SESSIONS.isEmpty()) return;

        Iterator<Map.Entry<Key, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Key, Session> entry = it.next();
            Key k = entry.getKey();
            if (!k.dim().equals(world.getRegistryKey())) continue;

            BlockPos pos = k.pos();
            if (!world.isChunkLoaded(pos)) continue;

            BlockState state = world.getBlockState(pos);
            if (!isDrillable(state)) {
                it.remove();
                continue;
            }

            Session cur = entry.getValue();
            if (cur.drill != null && !(world.getBlockState(cur.drill).getBlock() instanceof DrillBlock)) {
                it.remove();
                continue;
            }

            Session s = entry.getValue();
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;

            if (s.jammed) {
                if (world.getTime() % 10 == 0) {
                    world.spawnParticles(ParticleTypes.SMOKE, cx, cy + 0.4, cz, 4, 0.2, 0.2, 0.2, 0.01);
                    sendProgress(world, pos, s);
                }
                continue;
            }

            if (s.ticks > 40 && world.getRandom().nextInt(s.jamOdds) == 0) {
                s.jammed = true;
                world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.8f, 0.6f);
                continue;
            }

            s.ticks++;

            if (s.ticks % 8 == 0) {
                world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.7f, 1.7f);
                world.spawnParticles(ParticleTypes.CRIT, cx, cy, cz, 6, 0.3, 0.3, 0.3, 0.2);
            }
            if (s.ticks % 10 == 0) {
                sendProgress(world, pos, s);
            }
            if (state.getBlock() instanceof VaultDoorBlock && s.ticks % 60 == 0) {
                world.playSound(null, pos, SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 3.0f, 0.5f);
            }

            if (s.ticks >= s.total) {
                finish(world, pos, state);
                if (s.drill != null) broadcastStatus(world, s.drill, 3, 0, s.total);
                it.remove();
            }
        }
    }

    private static void sendProgress(ServerWorld world, BlockPos pos, Session s) {
        if (s.drill != null) broadcastStatus(world, s.drill, s.jammed ? 2 : 1, s.total - s.ticks, s.total);
        int pct = Math.min(100, s.ticks * 100 / s.total);
        Text text = s.jammed
                ? Text.literal("Бурение " + pct + "% — ЗАКЛИНИЛО! Нажми ПКМ чтобы починить").formatted(Formatting.RED)
                : Text.literal("Бурение: " + pct + "%").formatted(Formatting.YELLOW);
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 10 * 10) {
                p.sendMessage(text, true);
            }
        }
    }

    private static void finish(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LootSafeBlock) {
            LootSafeBlock.open(world, pos, state);
        } else if (state.getBlock() instanceof SafeBlock) {
            if (world.getBlockEntity(pos) instanceof SafeBlockEntity safe) {
                safe.setLocked(false);
            }
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 0.7f);
        } else if (state.getBlock() instanceof VaultDoorBlock) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos p = pos.up(dy);
                if (world.getBlockState(p).getBlock() instanceof VaultDoorBlock) {
                    world.breakBlock(p, false);
                }
            }
            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0f, 1.2f);
        }
        world.spawnParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.4, 0.4, 0.4, 0.05);
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 10 * 10) {
                p.sendMessage(Text.literal("Бурение завершено!").formatted(Formatting.GREEN), true);
            }
        }
    }
}
