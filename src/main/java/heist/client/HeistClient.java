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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ESCAPE_VAN, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SECURITY_DOOR, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DOOR_GRATE_LOCKED, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.C4, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ALARM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.READER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DRILL, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.THERMAL_DRILL, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.COMPUTER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CODE_PANEL, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MONEY_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOLD_PILE, RenderLayer.getCutout());

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_LOCKPICK, (client, handler, buf, responseSender) -> {
            buf.readBlockPos();
            int pins = buf.readVarInt();
            int halfPeriod = buf.readVarInt();
            int maxFails = buf.readVarInt();
            float[] center = new float[pins];
            float[] half = new float[pins];
            for (int i = 0; i < pins; i++) {
                center[i] = buf.readFloat();
                half[i] = buf.readFloat();
            }
            long start = buf.readLong();
            client.execute(() -> client.setScreen(new LockpickScreen(pins, halfPeriod, maxFails, center, half, start)));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.LOCKPICK_STATE, (client, handler, buf, responseSender) -> {
            buf.readBlockPos();
            int code = buf.readByte();
            int idx = buf.readVarInt();
            long start = buf.readLong();
            int fails = buf.readVarInt();
            client.execute(() -> {
                if (client.currentScreen instanceof LockpickScreen screen) {
                    screen.onState(code, idx, start, fails);
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(CameraView::tick);
        HudRenderCallback.EVENT.register(CameraView::renderHud);

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_KEYPAD, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            client.execute(() -> client.setScreen(new KeypadScreen(pos)));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_DRILL, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            client.execute(() -> client.setScreen(new DrillScreen(pos)));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.DRILL_STATUS, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int state = buf.readByte();
            int left = buf.readInt();
            int total = buf.readInt();
            client.execute(() -> {
                long now = client.world == null ? 0L : client.world.getTime();
                DrillClient.set(pos, state, left, total, now);
            });
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
