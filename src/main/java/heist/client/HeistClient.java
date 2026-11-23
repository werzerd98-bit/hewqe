package heist.client;

import heist.ModBlocks;
import heist.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class HeistClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CameraView.init();

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISPLAY_CASE, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISPLAY_CASE_FULL, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PAINTING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CAMERA, RenderLayer.getCutout());

        ClientTickEvents.END_CLIENT_TICK.register(CameraView::tick);
        HudRenderCallback.EVENT.register(CameraView::renderHud);

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_KEYPAD, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            client.execute(() -> client.setScreen(new KeypadScreen(pos)));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_COMPUTER, (client, handler, buf, responseSender) -> {
            int n = buf.readVarInt();
            List<CamInfo> cams = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                BlockPos p = buf.readBlockPos();
                float yaw = buf.readFloat();
                cams.add(new CamInfo(p, yaw));
            }
            client.execute(() -> client.setScreen(new ComputerScreen(cams)));
        });
    }
}
