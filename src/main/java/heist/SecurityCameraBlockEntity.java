package heist;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Пустой блок-сущность: нужен, чтобы реестр камер узнавал о загруженных камерах. */
public class SecurityCameraBlockEntity extends BlockEntity {
    public SecurityCameraBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMERA, pos, state);
    }
}
