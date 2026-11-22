package heist.client;

import heist.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Экран кодовой клавиатуры сейфа (4 цифры). Работает и мышью, и клавиатурой. */
public class KeypadScreen extends Screen {
    private final BlockPos pos;
    private final StringBuilder input = new StringBuilder();
    private int startY;

    public KeypadScreen(BlockPos pos) {
        super(Text.translatable("screen.heistmod.keypad"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK"};
        int startX = width / 2 - 58;
        startY = height / 2 - 36;
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            int x = startX + (i % 3) * 40;
            int y = startY + (i / 3) * 24;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> press(label))
                    .dimensions(x, y, 36, 20)
                    .build());
        }
    }

    private void press(String label) {
        switch (label) {
            case "C" -> input.setLength(0);
            case "OK" -> submit();
            default -> {
                if (input.length() < 4) input.append(label);
            }
        }
    }

    private void submit() {
        if (input.length() != 4) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(input.toString());
        ClientPlayNetworking.send(ModNetworking.SUBMIT_CODE, buf);
        close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= 48 && keyCode <= 57) {          // 0-9
            press(String.valueOf((char) keyCode));
            return true;
        }
        if (keyCode >= 320 && keyCode <= 329) {        // numpad 0-9
            press(String.valueOf(keyCode - 320));
            return true;
        }
        if (keyCode == 259 && input.length() > 0) {    // backspace
            input.setLength(input.length() - 1);
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {        // enter
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        StringBuilder shown = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            shown.append(i < input.length() ? input.charAt(i) : '_');
            if (i < 3) shown.append("  ");
        }
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, startY - 32, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(shown.toString()), width / 2, startY - 14, 0x55FF55);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
