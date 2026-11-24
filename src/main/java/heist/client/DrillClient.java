package heist.client;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Последнее состояние буров, присланное сервером (для окна бура). */
public final class DrillClient {
    private DrillClient() {}

    /** state: 0 — не работает, 1 — работает, 2 — заклинило, 3 — готово. */
    public record Status(int state, int left, int total, long receivedAt) {}

    private static final Map<BlockPos, Status> STATUS = new HashMap<>();

    public static void set(BlockPos pos, int state, int left, int total, long worldTime) {
        STATUS.put(pos.toImmutable(), new Status(state, left, total, worldTime));
    }

    public static Status get(BlockPos pos) {
        return STATUS.get(pos);
    }

    public static void clear() {
        STATUS.clear();
    }
}
