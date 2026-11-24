package heist.client;

import heist.DrillBlock;
import heist.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Окно бура: состояние, полоса прогресса и оставшееся время. */
public class DrillScreen extends Screen {
    private final BlockPos pos;
    private ButtonWidget startButton;
    private ButtonWidget fixButton;

    public DrillScreen(BlockPos pos) {
        super(Text.translatable("screen.heistmod.drill"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        startButton = addDrawableChild(ButtonWidget.builder(Text.literal("Запустить"), b -> send(0))
                .dimensions(cx - 50, cy + 30, 100, 20).build());
        fixButton = addDrawableChild(ButtonWidget.builder(Text.literal("Починить"), b -> send(1))
                .dimensions(cx - 50, cy + 30, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(cx - 50, cy + 54, 100, 20).build());
    }

    private void send(int action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeByte(action);
        ClientPlayNetworking.send(ModNetworking.DRILL_ACTION, buf);
    }

    private static String formatTime(int ticks) {
        int seconds = (int) Math.ceil(ticks / 20.0);
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client == null || client.world == null
                || !(client.world.getBlockState(pos).getBlock() instanceof DrillBlock drillBlock)) {
            close();
            return;
        }

        DrillClient.Status st = DrillClient.get(pos);
        int state = st == null ? 0 : st.state();
        int total = st == null ? 0 : st.total();
        int left = 0;
        if (st != null) {
            long passed = client.world.getTime() - st.receivedAt();
            left = state == 1 ? Math.max(0, st.left() - (int) passed) : st.left();
        }

        startButton.visible = state == 0;
        fixButton.visible = state == 2;

        renderBackground(context);

        int cx = width / 2;
        int cy = height / 2;
        context.fill(cx - 100, cy - 70, cx + 100, cy + 82, 0xE0141A20);
        context.fill(cx - 100, cy - 70, cx + 100, cy - 69, 0xFF5A6572);
        context.fill(cx - 100, cy + 81, cx + 100, cy + 82, 0xFF5A6572);

        context.drawCenteredTextWithShadow(textRenderer, drillBlock.getName(), cx, cy - 60, 0xFFFFFF);

        String statusText;
        int statusColor;
        switch (state) {
            case 1 -> { statusText = "Бурение идёт"; statusColor = 0xFFFFD84A; }
            case 2 -> { statusText = "ЗАКЛИНИЛО!"; statusColor = 0xFFFF5555; }
            case 3 -> { statusText = "Бурение завершено"; statusColor = 0xFF55FF55; }
            default -> { statusText = "Бур не работает"; statusColor = 0xFFAAAAAA; }
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(statusText), cx, cy - 44, statusColor);

        // полоса прогресса
        int barX = cx - 80;
        int barY = cy - 22;
        int barW = 160;
        float ratio = state == 3 ? 1.0f : (total > 0 ? 1.0f - (float) left / total : 0.0f);
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        context.fill(barX - 1, barY - 1, barX + barW + 1, barY + 11, 0xFF000000);
        context.fill(barX, barY, barX + barW, barY + 10, 0xFF2A2F36);
        context.fill(barX, barY, barX + (int) (barW * ratio), barY + 10, state == 2 ? 0xFFCC4444 : 0xFF4CAF50);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal((int) (ratio * 100) + "%"), cx, barY + 1, 0xFFFFFF);

        if (state == 1 || state == 2) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Осталось: " + formatTime(left)), cx, cy - 2, 0xFFFFFF);
        } else if (state == 3) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Осталось: 0:00"), cx, cy - 2, 0xFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Поставь бур вплотную к сейфу"), cx, cy - 2, 0xFFAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
