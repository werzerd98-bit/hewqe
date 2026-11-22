package heist;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
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

        Session(int total) {
            this.total = total;
        }
    }

    private static final int SAFE_TICKS = 20 * 20;   // 20 секунд
    private static final int VAULT_TICKS = 20 * 60;  // 60 секунд

    private static final Map<Key, Session> SESSIONS = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(DrillManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
    }

    public static boolean isDrillable(BlockState state) {
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
        player.sendMessage(Text.literal("Дрель починена!").formatted(Formatting.GREEN), true);
    }

    public static void start(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        boolean vault = state.getBlock() instanceof VaultDoorBlock;
        SESSIONS.put(key(world, pos), new Session(vault ? VAULT_TICKS : SAFE_TICKS));
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        if (vault) {
            alarm(world, pos);
        }
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

            if (s.ticks > 40 && world.getRandom().nextInt(1500) == 0) {
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
                it.remove();
            }
        }
    }

    private static void sendProgress(ServerWorld world, BlockPos pos, Session s) {
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
        if (state.getBlock() instanceof SafeBlock) {
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
