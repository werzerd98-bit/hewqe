package heist.client;

import heist.LockpickManager;
import heist.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

/** Мини-игра взлома: нажми ПРОБЕЛ (или кнопку), когда маркер в зелёной зоне. */
public class LockpickScreen extends Screen {
    private static final int BAR_W = 200;

    private final int pins;
    private final int halfPeriod;
    private final int maxFails;
    private final float[] center;
    private final float[] half;

    private int index;
    private long startTick;
    private int fails;
    private boolean finished;
    private String note = "Приготовься...";
    private long lastPress;

    public LockpickScreen(int pins, int halfPeriod, int maxFails, float[] center, float[] half, long startTick) {
        super(Text.translatable("screen.heistmod.lockpick"));
        this.pins = pins;
        this.halfPeriod = halfPeriod;
        this.maxFails = maxFails;
        this.center = center;
        this.half = half;
        this.startTick = startTick;
    }

    /** Ответ сервера: см. коды в LockpickManager. */
    public void onState(int code, int idx, long start, int failsCount) {
        switch (code) {
            case LockpickManager.CODE_PIN -> {
                index = idx;
                startTick = start;
                note = "Штифт встал!";
            }
            case LockpickManager.CODE_FAIL -> {
                index = 0;
                startTick = start;
                fails = failsCount;
                note = "Сорвалось! Начни заново";
            }
            default -> {
                finished = true;
                close();
            }
        }
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Нажать (Пробел)"), b -> press())
                .dimensions(width / 2 - 60, height / 2 + 38, 120, 20).build());
    }

    private void press() {
        long now = System.currentTimeMillis();
        if (now - lastPress < 150) return;
        lastPress = now;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(0);
        ClientPlayNetworking.send(ModNetworking.LOCKPICK_ACTION, buf);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32 || keyCode == 257) { // пробел, Enter
            press();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        if (!finished) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeByte(1);
            ClientPlayNetworking.send(ModNetworking.LOCKPICK_ACTION, buf);
        }
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client == null || client.world == null) {
            finished = true;
            close();
            return;
        }
        renderBackground(context);

        int cx = width / 2;
        int cy = height / 2;
        context.fill(cx - 120, cy - 66, cx + 120, cy + 66, 0xE0141A20);
        context.fill(cx - 120, cy - 66, cx + 120, cy - 65, 0xFF5A6572);
        context.fill(cx - 120, cy + 65, cx + 120, cy + 66, 0xFF5A6572);
        context.drawCenteredTextWithShadow(textRenderer, title, cx, cy - 58, 0xFFFFFF);

        // штифты
        int pinsW = pins * 16;
        for (int i = 0; i < pins; i++) {
            int x = cx - pinsW / 2 + i * 16 + 3;
            int color = i < index ? 0xFF4CAF50 : (i == index ? 0xFFFFD84A : 0xFF4A4F57);
            int h = i < index ? 12 : 26;
            context.fill(x, cy - 42 + (26 - h), x + 10, cy - 16, color);
        }

        // полоса с зоной и маркером
        int x0 = cx - BAR_W / 2;
        int y0 = cy + 2;
        context.fill(x0 - 1, y0 - 1, x0 + BAR_W + 1, y0 + 17, 0xFF000000);
        context.fill(x0, y0, x0 + BAR_W, y0 + 16, 0xFF2A2F36);
        int zl = x0 + Math.round((center[index] - half[index]) * BAR_W);
        int zr = x0 + Math.round((center[index] + half[index]) * BAR_W);
        context.fill(zl, y0, zr, y0 + 16, 0xFF2E7D32);

        double t = client.world.getTime() + delta - startTick;
        float m = LockpickManager.marker((long) Math.floor(Math.max(t, 0)), halfPeriod);
        // плавное движение между тиками
        if (t > 0) {
            double phase = (t % (2.0 * halfPeriod)) / halfPeriod;
            m = (float) (phase <= 1.0 ? phase : 2.0 - phase);
        }
        int mx = x0 + Math.round(m * BAR_W);
        context.fill(mx - 1, y0 - 3, mx + 1, y0 + 19, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal(note), cx, cy + 22, 0xFFFFD84A);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Срывов: " + fails + " / " + maxFails), cx, cy + 62 - 12, 0xFFAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
