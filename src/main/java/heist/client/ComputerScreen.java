package heist.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Экран компьютера охраны: список камер. */
public class ComputerScreen extends Screen {
    private final List<CamInfo> cams;

    public ComputerScreen(List<CamInfo> cams) {
        super(Text.translatable("screen.heistmod.computer"));
        this.cams = cams;
    }

    @Override
    protected void init() {
        int startX = width / 2 - 125;
        int startY = height / 2 - 80;
        for (int i = 0; i < cams.size(); i++) {
            final int idx = i;
            CamInfo info = cams.get(i);
            Text label = Text.literal("Камера " + (i + 1) + "  [" + info.pos().getX() + ", "
                    + info.pos().getY() + ", " + info.pos().getZ() + "]");
            int x = startX + (i / 8) * 130;
            int y = startY + (i % 8) * 22;
            addDrawableChild(ButtonWidget.builder(label, b -> {
                        client.setScreen(null);
                        CameraView.start(client, cams, idx);
                    })
                    .dimensions(x, y, 120, 20)
                    .build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 105, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
