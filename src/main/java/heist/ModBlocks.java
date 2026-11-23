package heist;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.GlassBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Block DRILL = register("drill",
            new DrillBlock(FabricBlockSettings.create().strength(2.0f).sounds(BlockSoundGroup.METAL)));

    public static final Block SAFE = register("safe",
            new SafeBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL)));

    public static final Block VAULT_DOOR = register("vault_door",
            new VaultDoorBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL)));

    public static final Block READER_GREEN = register("keycard_reader_green",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(3.0f, 6.0f).sounds(BlockSoundGroup.METAL), 1));

    public static final Block READER_BLUE = register("keycard_reader_blue",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(3.0f, 6.0f).sounds(BlockSoundGroup.METAL), 2));

    public static final Block READER_RED = register("keycard_reader_red",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(3.0f, 6.0f).sounds(BlockSoundGroup.METAL), 3));

    /** Пустая витрина: просто стекло. */
    public static final Block DISPLAY_CASE = register("display_case",
            new GlassBlock(FabricBlockSettings.create().strength(0.4f).nonOpaque().sounds(BlockSoundGroup.GLASS)));

    /** Полная витрина: при разбитии даёт украшения/золото/деньги. */
    public static final Block DISPLAY_CASE_FULL = register("display_case_full",
            new GlassBlock(FabricBlockSettings.create().strength(0.4f).nonOpaque().sounds(BlockSoundGroup.GLASS)));

    public static final Block MONEY_BLOCK = register("money_block",
            new Block(FabricBlockSettings.create().strength(1.0f).sounds(BlockSoundGroup.WOOL)));

    public static final Block GOLD_PILE = register("gold_pile",
            new Block(FabricBlockSettings.create().strength(2.0f, 6.0f).sounds(BlockSoundGroup.METAL)));

    public static final Block PAINTING = register("painting",
            new PaintingBlock(FabricBlockSettings.create().strength(0.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.WOOD)),
            new FabricItemSettings().maxCount(16));

    public static final Block CAMERA = register("security_camera",
            new SecurityCameraBlock(FabricBlockSettings.create().strength(2.0f).nonOpaque().sounds(BlockSoundGroup.METAL)));

    public static final Block COMPUTER = register("security_computer",
            new SecurityComputerBlock(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.METAL)));

    private static Block register(String name, Block block) {
        return register(name, block, new FabricItemSettings());
    }

    private static Block register(String name, Block block, Item.Settings itemSettings) {
        Registry.register(Registries.BLOCK, HeistMod.id(name), block);
        Registry.register(Registries.ITEM, HeistMod.id(name), new BlockItem(block, itemSettings));
        return block;
    }

    public static void init() {}
}
