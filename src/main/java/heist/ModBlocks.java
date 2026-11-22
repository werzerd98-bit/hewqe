package heist;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.GlassBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Block SAFE = register("safe",
            new SafeBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL)));

    public static final Block VAULT_DOOR = register("vault_door",
            new VaultDoorBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL)));

    public static final Block READER_GREEN = register("keycard_reader_green",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(5.0f, 6.0f).requiresTool().sounds(BlockSoundGroup.METAL), 1));

    public static final Block READER_BLUE = register("keycard_reader_blue",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(5.0f, 6.0f).requiresTool().sounds(BlockSoundGroup.METAL), 2));

    public static final Block READER_RED = register("keycard_reader_red",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(5.0f, 6.0f).requiresTool().sounds(BlockSoundGroup.METAL), 3));

    public static final Block DISPLAY_CASE = register("display_case",
            new GlassBlock(FabricBlockSettings.create().strength(0.4f).nonOpaque().sounds(BlockSoundGroup.GLASS)));

    private static Block register(String name, Block block) {
        Registry.register(Registries.BLOCK, HeistMod.id(name), block);
        Registry.register(Registries.ITEM, HeistMod.id(name), new BlockItem(block, new FabricItemSettings()));
        return block;
    }

    public static void init() {}
}
