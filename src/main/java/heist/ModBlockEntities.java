package heist;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final BlockEntityType<SafeBlockEntity> SAFE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            HeistMod.id("safe"),
            FabricBlockEntityTypeBuilder.create(SafeBlockEntity::new, ModBlocks.SAFE).build());

    public static void init() {}
}
