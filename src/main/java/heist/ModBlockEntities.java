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

    public static final BlockEntityType<SecurityCameraBlockEntity> CAMERA = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            HeistMod.id("security_camera"),
            FabricBlockEntityTypeBuilder.create(SecurityCameraBlockEntity::new, ModBlocks.CAMERA).build());

    public static final BlockEntityType<CodePanelBlockEntity> PANEL = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            HeistMod.id("code_panel"),
            FabricBlockEntityTypeBuilder.create(CodePanelBlockEntity::new, ModBlocks.CODE_PANEL).build());

    public static final BlockEntityType<EscapeVanBlockEntity> VAN = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            HeistMod.id("escape_van"),
            FabricBlockEntityTypeBuilder.create(EscapeVanBlockEntity::new, ModBlocks.ESCAPE_VAN).build());

    public static void init() {}
}
