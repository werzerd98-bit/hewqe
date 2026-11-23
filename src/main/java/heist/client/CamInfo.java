package heist.client;

import net.minecraft.util.math.BlockPos;

/** Одна камера из списка, присланного сервером. */
public record CamInfo(BlockPos pos, float yaw) {}
