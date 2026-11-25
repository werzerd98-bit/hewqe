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
            new DrillBlock(FabricBlockSettings.create().strength(2.0f).nonOpaque().sounds(BlockSoundGroup.METAL), DrillType.DRILL));

    public static final Block THERMAL_DRILL = register("thermal_drill",
            new DrillBlock(FabricBlockSettings.create().strength(2.0f).nonOpaque().sounds(BlockSoundGroup.METAL), DrillType.THERMAL));

    public static final Block SAFE = register("safe",
            new SafeBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL)));

    /** Сейфы с добычей: открываются только сверлением. */
    public static final Block SAFE_GOLD = register("safe_gold",
            new LootSafeBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).nonOpaque().sounds(BlockSoundGroup.METAL), LootBagItem.KIND_GOLD));

    public static final Block SAFE_MONEY = register("safe_money",
            new LootSafeBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).nonOpaque().sounds(BlockSoundGroup.METAL), LootBagItem.KIND_MONEY));

    public static final Block VAULT_DOOR = register("vault_door",
            new VaultDoorBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL)));

    /** Единственный считыватель ключ-карт: небольшая панель на стене. */
    public static final Block READER = register("keycard_reader",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(3.0f, 6.0f).nonOpaque().noCollision().sounds(BlockSoundGroup.METAL)));

    /** Пустая витрина: просто стекло. */
    public static final Block DISPLAY_CASE = register("display_case",
            new GlassBlock(FabricBlockSettings.create().strength(0.4f).nonOpaque().sounds(BlockSoundGroup.GLASS)));

    /** Полная витрина: при разбитии даёт только украшения. */
    public static final Block DISPLAY_CASE_FULL = register("display_case_full",
            new GlassBlock(FabricBlockSettings.create().strength(0.4f).nonOpaque().sounds(BlockSoundGroup.GLASS)));

    public static final Block MONEY_BLOCK = register("money_block",
            new LootBlock(FabricBlockSettings.create().strength(1.0f).nonOpaque().sounds(BlockSoundGroup.WOOL), LootBagItem.KIND_MONEY));

    public static final Block GOLD_PILE = register("gold_pile",
            new LootBlock(FabricBlockSettings.create().strength(2.0f, 6.0f).nonOpaque().sounds(BlockSoundGroup.METAL), LootBagItem.KIND_GOLD));

    public static final Block PAINTING = register("painting",
            new PaintingBlock(FabricBlockSettings.create().strength(0.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.WOOD)),
            new FabricItemSettings().maxCount(16));

    public static final Block CAMERA = register("security_camera",
            new SecurityCameraBlock(FabricBlockSettings.create().strength(2.0f).nonOpaque().sounds(BlockSoundGroup.METAL)));

    public static final Block COMPUTER = register("security_computer",
            new SecurityComputerBlock(FabricBlockSettings.create().strength(3.0f).nonOpaque().sounds(BlockSoundGroup.METAL)));

    public static final Block CASH_REGISTER = register("cash_register",
            new CashRegisterBlock(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.METAL)));

    /** Секция большой двери: неразрушаема в выживании, открывается сигналом редстоуна. */
    public static final Block SECURITY_DOOR = register("security_door",
            new SecurityDoorBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).nonOpaque().sounds(BlockSoundGroup.METAL)));

    public static final Block CODE_PANEL = register("code_panel",
            new CodePanelBlock(FabricBlockSettings.create().strength(3.0f, 6.0f).nonOpaque().noCollision().sounds(BlockSoundGroup.METAL)));

    public static final Block ESCAPE_VAN = register("escape_van",
            new EscapeVanBlock(FabricBlockSettings.create().strength(5.0f, 6.0f).nonOpaque().sounds(BlockSoundGroup.METAL)));

    /** Запертые двери: вскрываются отмычкой (число штифтов = сложность). */
    public static final Block DOOR_WOOD_LOCKED = register("door_wood_locked",
            new PickDoorBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).nonOpaque().sounds(BlockSoundGroup.WOOD), 3));
    public static final Block DOOR_STEEL_LOCKED = register("door_steel_locked",
            new PickDoorBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).nonOpaque().sounds(BlockSoundGroup.METAL), 4));
    public static final Block DOOR_GRATE_LOCKED = register("door_grate_locked",
            new PickDoorBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).nonOpaque().sounds(BlockSoundGroup.CHAIN), 5));

    /** Двери под взрывчатку. */
    public static final Block DOOR_BREACH_WOOD = register("door_breach_wood",
            new BreachDoorBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.WOOD)));
    public static final Block DOOR_BREACH_STEEL = register("door_breach_steel",
            new BreachDoorBlock(FabricBlockSettings.create().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL)));

    /** Сигнализация: настенная панель. */
    public static final Block ALARM = register("alarm",
            new AlarmBlock(FabricBlockSettings.create().strength(2.0f).nonOpaque().noCollision().sounds(BlockSoundGroup.METAL)));

    /** Игровая (ненастоящая) взрывчатка. */
    public static final Block C4 = register("c4",
            new C4Block(FabricBlockSettings.create().strength(0.5f).noCollision().nonOpaque().sounds(BlockSoundGroup.METAL)));

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
