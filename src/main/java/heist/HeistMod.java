package heist;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HeistMod implements ModInitializer {
    public static final String MOD_ID = "heistmod";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, id("main"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.heistmod.main"))
                    .icon(() -> new ItemStack(ModBlocks.DRILL))
                    .entries((ctx, entries) -> {
                        entries.add(ModBlocks.DRILL);
                        entries.add(ModBlocks.THERMAL_DRILL);
                        entries.add(ModItems.KEYCARD);
                        entries.add(ModBlocks.SAFE);
                        entries.add(ModBlocks.SAFE_GOLD);
                        entries.add(ModBlocks.SAFE_MONEY);
                        entries.add(ModBlocks.VAULT_DOOR);
                        entries.add(ModBlocks.READER);
                        entries.add(ModBlocks.SECURITY_DOOR);
                        entries.add(ModBlocks.CODE_PANEL);
                        entries.add(ModItems.LOCKPICK);
                        entries.add(ModBlocks.DOOR_WOOD_LOCKED);
                        entries.add(ModBlocks.DOOR_STEEL_LOCKED);
                        entries.add(ModBlocks.DOOR_GRATE_LOCKED);
                        entries.add(ModBlocks.DOOR_BREACH_WOOD);
                        entries.add(ModBlocks.DOOR_BREACH_STEEL);
                        entries.add(ModBlocks.C4);
                        entries.add(ModBlocks.ESCAPE_VAN);
                        entries.add(ModBlocks.CAMERA);
                        entries.add(ModBlocks.COMPUTER);
                        entries.add(ModBlocks.DISPLAY_CASE);
                        entries.add(ModBlocks.DISPLAY_CASE_FULL);
                        entries.add(ModBlocks.PAINTING);
                        entries.add(ModBlocks.CASH_REGISTER);
                        entries.add(ModBlocks.MONEY_BLOCK);
                        entries.add(ModBlocks.GOLD_PILE);
                        entries.add(ModItems.GOLD_BAR);
                        entries.add(ModItems.CASH_BUNDLE);
                        entries.add(ModItems.JEWELRY);
                        entries.add(LootBagItem.create(LootBagItem.KIND_MONEY, 4));
                        entries.add(LootBagItem.create(LootBagItem.KIND_GOLD, 4));
                    })
                    .build());

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModNetworking.registerServer();
        LockpickManager.init();
        DrillManager.init();
        CameraRegistry.init();
        LootBags.init();
        HeistCommand.register();
    }
}
