package heist;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * /heist build bank      — построить банк перед игроком
 * /heist build jewelry   — построить ювелирный магазин
 * /heist kit             — выдать дрель и ключ-карты
 * /heist note            — записка с кодом сейфа, на который смотришь
 */
public final class HeistCommand {
    private HeistCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("heist")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("build")
                                .then(CommandManager.literal("bank").executes(ctx -> build(ctx.getSource(), true)))
                                .then(CommandManager.literal("jewelry").executes(ctx -> build(ctx.getSource(), false))))
                        .then(CommandManager.literal("kit").executes(ctx -> kit(ctx.getSource())))
                        .then(CommandManager.literal("note").executes(ctx -> note(ctx.getSource())))));
    }

    private static int build(ServerCommandSource src, boolean bank) throws CommandSyntaxException {
        ServerPlayerEntity player = src.getPlayerOrThrow();
        ServerWorld world = src.getWorld();
        BlockPos base = player.getBlockPos().down();

        if (bank) {
            BuildingBuilder.bank(world, base.add(-10, 0, 3));
            src.sendFeedback(() -> Text.literal("Банк построен перед тобой (вход смотрит на север). Начни с сундука за стойкой в зале!"), true);
        } else {
            BuildingBuilder.jewelry(world, base.add(-6, 0, 3));
            src.sendFeedback(() -> Text.literal("Ювелирный магазин построен перед тобой. Ключ-карта лежит в кассе за прилавком."), true);
        }
        return 1;
    }

    private static int kit(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity player = src.getPlayerOrThrow();
        player.giveItemStack(new ItemStack(ModBlocks.DRILL));
        player.giveItemStack(new ItemStack(ModBlocks.CAMERA, 2));
        player.giveItemStack(new ItemStack(ModBlocks.COMPUTER));
        player.giveItemStack(new ItemStack(ModBlocks.PAINTING, 2));
        player.giveItemStack(new ItemStack(ModItems.KEYCARD_GREEN));
        player.giveItemStack(new ItemStack(ModItems.KEYCARD_BLUE));
        player.giveItemStack(new ItemStack(ModItems.KEYCARD_RED));
        src.sendFeedback(() -> Text.literal("Выдан набор: бур, камеры, компьютер, картины и три ключ-карты."), false);
        return 1;
    }

    private static int note(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity player = src.getPlayerOrThrow();
        HitResult hit = player.raycast(8.0, 0.0f, false);
        if (hit instanceof BlockHitResult bhr
                && src.getWorld().getBlockEntity(bhr.getBlockPos()) instanceof SafeBlockEntity safe) {
            player.giveItemStack(ModItems.codeNote(safe.getCode(), "Код от сейфа"));
            src.sendFeedback(() -> Text.literal("Записка с кодом выдана."), false);
            return 1;
        }
        src.sendError(Text.literal("Смотри на сейф (до 8 блоков)."));
        return 0;
    }
}
