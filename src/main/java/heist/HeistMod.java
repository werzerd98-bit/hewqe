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
                    .icon(() -> new ItemStack(ModItems.DRILL))
                    .entries((ctx, entries) -> {
                        entries.add(ModItems.DRILL);
                        entries.add(ModItems.KEYCARD_GREEN);
                        entries.add(ModItems.KEYCARD_BLUE);
                        entries.add(ModItems.KEYCARD_RED);
                        entries.add(ModBlocks.SAFE);
                        entries.add(ModBlocks.VAULT_DOOR);
                        entries.add(ModBlocks.READER_GREEN);
                        entries.add(ModBlocks.READER_BLUE);
                        entries.add(ModBlocks.READER_RED);
                        entries.add(ModBlocks.DISPLAY_CASE);
                        entries.add(ModItems.GOLD_BAR);
                        entries.add(ModItems.CASH_BUNDLE);
                        entries.add(ModItems.JEWELRY);
                        entries.add(ModItems.PAINTING);
                    })
                    .build());

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModNetworking.registerServer();
        DrillManager.init();
        HeistCommand.register();
    }
}
