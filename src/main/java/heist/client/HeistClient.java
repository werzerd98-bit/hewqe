package heist.client;

import heist.ModBlocks;
import heist.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.BlockPos;

public class HeistClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISPLAY_CASE, RenderLayer.getTranslucent());

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_KEYPAD, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            client.execute(() -> client.setScreen(new KeypadScreen(pos)));
        });
    }
}
