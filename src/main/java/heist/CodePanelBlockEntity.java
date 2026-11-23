package heist;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

/** Хранит код кодовой панели. */
public class CodePanelBlockEntity extends BlockEntity {
    private String code = "0000";

    public CodePanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PANEL, pos, state);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        markDirty();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        code = nbt.contains("Code") ? nbt.getString("Code") : "0000";
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Code", code);
    }
}
