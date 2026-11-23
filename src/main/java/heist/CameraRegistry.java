package heist;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Хранит позиции всех загруженных камер, чтобы компьютер мог их показать. */
public final class CameraRegistry {
    private CameraRegistry() {}

    public static final int MAX_CAMERAS = 16;

    private static final Map<RegistryKey<World>, Set<BlockPos>> CAMERAS = new HashMap<>();

    public static void init() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
            if (blockEntity instanceof SecurityCameraBlockEntity) {
                CAMERAS.computeIfAbsent(world.getRegistryKey(), k -> new HashSet<>()).add(blockEntity.getPos().toImmutable());
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
            if (blockEntity instanceof SecurityCameraBlockEntity) {
                Set<BlockPos> set = CAMERAS.get(world.getRegistryKey());
                if (set != null) set.remove(blockEntity.getPos());
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CAMERAS.clear());
    }

    /** Камеры в радиусе range блоков от center, ближайшие первыми. */
    public static List<BlockPos> near(ServerWorld world, BlockPos center, double range) {
        Set<BlockPos> set = CAMERAS.get(world.getRegistryKey());
        List<BlockPos> result = new ArrayList<>();
        if (set == null) return result;

        double max = range * range;
        for (BlockPos p : set) {
            if (p.getSquaredDistance(center) <= max && world.getBlockState(p).getBlock() instanceof SecurityCameraBlock) {
                result.add(p);
            }
        }
        result.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(center)));
        if (result.size() > MAX_CAMERAS) {
            return new ArrayList<>(result.subList(0, MAX_CAMERAS));
        }
        return result;
    }
}
