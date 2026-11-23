package heist.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Просмотр камеры на стороне клиента: камера игры переключается на невидимую сущность
 * в позиции камеры. Тело игрока остаётся у компьютера. Выход — клавиша V, шаг в сторону или урон.
 */
public final class CameraView {
    private CameraView() {}

    // GLFW: V = 86, B = 66
    public static final KeyBinding EXIT = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.heistmod.exit_camera", InputUtil.Type.KEYSYM, 86, "key.categories.heistmod"));
    public static final KeyBinding NEXT = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.heistmod.next_camera", InputUtil.Type.KEYSYM, 66, "key.categories.heistmod"));

    private static boolean active;
    private static List<CamInfo> cams = List.of();
    private static int index;
    private static Entity camEntity;
    private static Vec3d startPos = Vec3d.ZERO;
    private static float baseYaw;
    private static int ticks;

    public static void init() {
        // вызов нужен, чтобы класс загрузился и клавиши зарегистрировались до чтения настроек
    }

    public static void start(MinecraftClient client, List<CamInfo> list, int startIndex) {
        if (client.player == null || client.world == null || list.isEmpty()) return;

        cams = list;
        index = Math.floorMod(startIndex, list.size());
        startPos = client.player.getPos();
        ticks = 0;

        camEntity = EntityType.MARKER.create(client.world);
        if (camEntity == null) return;
        applyCurrent();
        active = true;
        client.setCameraEntity(camEntity);
    }

    private static void applyCurrent() {
        CamInfo info = cams.get(index);
        Direction dir = Direction.fromRotation(info.yaw());
        double x = info.pos().getX() + 0.5 + dir.getOffsetX() * 0.6;
        double y = info.pos().getY() + 0.6;
        double z = info.pos().getZ() + 0.5 + dir.getOffsetZ() * 0.6;
        baseYaw = info.yaw();

        camEntity.refreshPositionAndAngles(x, y, z, baseYaw, 15.0f);
        camEntity.prevX = x;
        camEntity.prevY = y;
        camEntity.prevZ = z;
        camEntity.prevYaw = baseYaw;
        camEntity.prevPitch = 15.0f;
    }

    public static boolean isActive() {
        return active;
    }

    public static void stop(MinecraftClient client) {
        if (!active) return;
        active = false;
        camEntity = null;
        if (client.player != null) {
            client.setCameraEntity(client.player);
        }
    }

    public static void tick(MinecraftClient client) {
        // клавиши читаем всегда, чтобы очередь нажатий не копилась
        boolean exit = false;
        boolean next = false;
        while (EXIT.wasPressed()) exit = true;
        while (NEXT.wasPressed()) next = true;

        if (!active) return;
        if (client.player == null || client.world == null) {
            active = false;
            camEntity = null;
            return;
        }
        if (exit
                || client.player.hurtTime > 0
                || client.player.getPos().squaredDistanceTo(startPos) > 2.25) {
            stop(client);
            return;
        }
        if (next) {
            index = (index + 1) % cams.size();
            applyCurrent();
        }

        ticks++;
        camEntity.prevYaw = camEntity.getYaw();
        camEntity.setYaw(baseYaw + 25.0f * (float) Math.sin(ticks * 0.04));
    }

    public static void renderHud(DrawContext ctx, float tickDelta) {
        if (!active) return;
        MinecraftClient c = MinecraftClient.getInstance();
        int w = c.getWindow().getScaledWidth();
        int h = c.getWindow().getScaledHeight();

        ctx.fill(0, 0, w, 16, 0x99000000);
        ctx.fill(0, h - 16, w, h, 0x99000000);
        ctx.drawTextWithShadow(c.textRenderer,
                Text.literal("● REC   КАМЕРА " + (index + 1) + " / " + cams.size()), 8, 4, 0xFFFF5555);
        ctx.drawTextWithShadow(c.textRenderer,
                Text.literal("[B] следующая камера    [V] выход (или отойди от компьютера)"), 8, h - 12, 0xFFFFFFFF);
    }
}
