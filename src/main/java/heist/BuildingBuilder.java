package heist;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Строит готовые здания для ограбления.
 * Локальные координаты: x — на восток, z — на юг, y — вверх; y=0 это пол.
 */
public final class BuildingBuilder {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private final ServerWorld w;
    private final BlockPos o;
    private final Random r;

    private BuildingBuilder(ServerWorld w, BlockPos origin) {
        this.w = w;
        this.o = origin;
        this.r = w.getRandom();
    }

    public static void bank(ServerWorld w, BlockPos origin) {
        new BuildingBuilder(w, origin).buildBank();
    }

    public static void door(ServerWorld w, BlockPos origin) {
        new BuildingBuilder(w, origin).buildDoor();
    }

    public static void doors(ServerWorld w, BlockPos origin) {
        new BuildingBuilder(w, origin).buildDoors();
    }

    public static void jewelry(ServerWorld w, BlockPos origin) {
        new BuildingBuilder(w, origin).buildJewelry();
    }

    private void twoHigh(int x, int z, BlockState door) {
        EnumProperty<DoubleBlockHalf> half = Properties.DOUBLE_BLOCK_HALF;
        set(x, 1, z, door.with(half, DoubleBlockHalf.LOWER));
        set(x, 2, z, door.with(half, DoubleBlockHalf.UPPER));
    }

    /** Демо-стена: три запертые двери и две под взрывчатку. Сторона входа — север (z меньше). */
    private void buildDoors() {
        BlockState wall = Blocks.STONE_BRICKS.getDefaultState();
        fill(-1, 0, 0, 24, 0, 3, Blocks.SMOOTH_STONE.getDefaultState());
        fill(-1, 1, 0, 24, 4, 0, wall);
        BlockState face = AIR;
        // x: 2 дерево, 6 сталь, 10 решётка, 14-15 взрывчатка (дерево), 19-20 взрывчатка (сталь)
        BlockState wood = ModBlocks.DOOR_WOOD_LOCKED.getDefaultState().with(PickDoorBlock.FACING, Direction.NORTH);
        BlockState steel = ModBlocks.DOOR_STEEL_LOCKED.getDefaultState().with(PickDoorBlock.FACING, Direction.NORTH);
        BlockState grate = ModBlocks.DOOR_GRATE_LOCKED.getDefaultState().with(PickDoorBlock.FACING, Direction.NORTH);
        twoHigh(2, 0, wood);
        twoHigh(6, 0, steel);
        twoHigh(10, 0, grate);
        twoHigh(14, 0, ModBlocks.DOOR_BREACH_WOOD.getDefaultState());
        twoHigh(15, 0, ModBlocks.DOOR_BREACH_WOOD.getDefaultState());
        twoHigh(19, 0, ModBlocks.DOOR_BREACH_STEEL.getDefaultState());
        twoHigh(20, 0, ModBlocks.DOOR_BREACH_STEEL.getDefaultState());
        fill(-1, 1, 1, 24, 3, 3, face);

        // сундук с инструментами за дверью №1
        set(0, 1, 2, Blocks.CHEST.getDefaultState());
        if (w.getBlockEntity(o.add(0, 1, 2)) instanceof Inventory inv) {
            inv.setStack(0, new ItemStack(ModItems.LOCKPICK, 8));
            inv.setStack(1, new ItemStack(ModBlocks.C4, 6));
        }
        set(1, 1, 2, Blocks.LANTERN.getDefaultState());
    }

    // ------------------------------------------------------------------ helpers

    private void set(int x, int y, int z, BlockState s) {
        w.setBlockState(o.add(x, y, z), s, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, s);
    }

    private void ironDoor(int x, int y, int z, Direction facing) {
        BlockState base = Blocks.IRON_DOOR.getDefaultState().with(Properties.HORIZONTAL_FACING, facing);
        set(x, y, z, base.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, base.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
    }

    /** Стена остаётся целой, считыватель вешается на неё с обеих сторон (front и противоположной). */
    private void reader(int x, int y, int z, Direction front) {
        BlockPos wall = o.add(x, y, z);
        if (!w.getBlockState(wall).isOpaqueFullCube(w, wall)) {
            set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
        }
        for (Direction d : new Direction[]{front, front.getOpposite()}) {
            BlockPos p = wall.offset(d);
            if (w.getBlockState(p).isAir()) {
                w.setBlockState(p, ModBlocks.READER.getDefaultState().with(KeycardReaderBlock.FACING, d),
                        Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        }
    }

    private void vaultDoor(int x, int z) {
        set(x, 1, z, ModBlocks.VAULT_DOOR.getDefaultState());
        set(x, 2, z, ModBlocks.VAULT_DOOR.getDefaultState());
    }

    private void lamp(int x, int y, int z) {
        set(x, y, z, Blocks.SEA_LANTERN.getDefaultState());
    }

    private void painting(int x, int y, int z, Direction facing) {
        set(x, y, z, ModBlocks.PAINTING.getDefaultState().with(PaintingBlock.FACING, facing));
    }

    private void cashRegister(int x, int y, int z, Direction facing) {
        set(x, y, z, ModBlocks.CASH_REGISTER.getDefaultState().with(CashRegisterBlock.FACING, facing));
    }

    /** Стена остаётся целой, панель вешается на её бок (face — сторона, куда смотрит панель). */
    private void panel(int x, int y, int z, Direction face, String code) {
        BlockPos wall = o.add(x, y, z);
        if (!w.getBlockState(wall).isOpaqueFullCube(w, wall)) {
            set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
        }
        BlockPos p = wall.offset(face);
        w.setBlockState(p, ModBlocks.CODE_PANEL.getDefaultState().with(CodePanelBlock.FACING, face),
                Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        if (w.getBlockEntity(p) instanceof CodePanelBlockEntity panel) {
            panel.setCode(code);
        }
    }

    private void lootSafe(int x, int y, int z, Direction face, boolean gold) {
        set(x, y, z, (gold ? ModBlocks.SAFE_GOLD : ModBlocks.SAFE_MONEY).getDefaultState()
                .with(LootSafeBlock.FACING, face));
    }

    private void camera(int x, int y, int z, Direction facing) {
        set(x, y, z, ModBlocks.CAMERA.getDefaultState().with(SecurityCameraBlock.FACING, facing));
    }

    private void computer(int x, int y, int z, Direction facing) {
        set(x, y, z, ModBlocks.COMPUTER.getDefaultState().with(SecurityComputerBlock.FACING, facing));
    }

    private void hangingLantern(int x, int y, int z) {
        set(x, y, z, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
    }

    private String randomCode() {
        return String.format("%04d", r.nextInt(10000));
    }

    private SafeBlockEntity safe(int x, int y, int z, Direction face, String code) {
        set(x, y, z, ModBlocks.SAFE.getDefaultState().with(SafeBlock.FACING, face));
        if (w.getBlockEntity(o.add(x, y, z)) instanceof SafeBlockEntity safe) {
            safe.setCode(code);
            safe.setLocked(true);
            return safe;
        }
        return null;
    }

    private void addTo(SafeBlockEntity safe, ItemStack... stacks) {
        if (safe == null) return;
        for (ItemStack s : stacks) safe.addLoot(s, r);
    }

    private void chest(int x, int y, int z, Direction face, ItemStack... stacks) {
        set(x, y, z, Blocks.CHEST.getDefaultState().with(Properties.HORIZONTAL_FACING, face));
        if (w.getBlockEntity(o.add(x, y, z)) instanceof Inventory inv) {
            for (int i = 0; i < stacks.length && i < inv.size(); i++) {
                inv.setStack(i, stacks[i]);
            }
        }
    }

    private ItemStack cash(int n) {
        return new ItemStack(ModItems.CASH_BUNDLE, n);
    }

    private ItemStack gold(int n) {
        return new ItemStack(ModItems.GOLD_BAR, n);
    }

    // ------------------------------------------------------------------ BANK

    private void buildBank() {
        BlockState brick = Blocks.STONE_BRICKS.getDefaultState();
        BlockState glass = Blocks.GLASS.getDefaultState();
        BlockState reinforced = Blocks.REINFORCED_DEEPSLATE.getDefaultState();

        // Расчистка, пол, стены, крыша
        fill(-1, 1, -1, 21, 7, 15, AIR);
        fill(0, 0, 0, 20, 0, 14, Blocks.POLISHED_ANDESITE.getDefaultState());
        fill(1, 0, 1, 19, 0, 7, Blocks.POLISHED_DIORITE.getDefaultState());
        fill(0, 1, 0, 20, 4, 14, brick);
        fill(1, 1, 1, 19, 4, 13, AIR);
        fill(0, 5, 0, 20, 5, 14, brick);

        // Вход и окна
        fill(9, 1, 0, 11, 2, 0, AIR);
        fill(3, 2, 0, 6, 3, 0, glass);
        fill(14, 2, 0, 17, 3, 0, glass);

        // Стойка кассиров
        fill(3, 1, 3, 17, 1, 3, Blocks.QUARTZ_BLOCK.getDefaultState());
        fill(3, 2, 3, 9, 3, 3, glass);
        fill(11, 2, 3, 17, 3, 3, glass);
        set(10, 1, 3, AIR);

        // Стена между залом и служебными помещениями
        fill(1, 1, 8, 19, 4, 8, brick);
        // Стена кабинета менеджера (x=6) и стена хранилища (x=13)
        fill(6, 1, 9, 6, 4, 13, brick);

        // Хранилище: армированный сланец (в режиме выживания не ломается)
        fill(13, 0, 8, 20, 5, 14, reinforced);
        fill(14, 1, 9, 19, 4, 13, AIR);

        String codeA = randomCode();
        String codeB = randomCode();

        // 1) Зал: дверь в служебную зону — зелёная карта
        ironDoor(10, 1, 8, Direction.NORTH);
        reader(9, 1, 8, Direction.NORTH);
        reader(11, 1, 8, Direction.NORTH);

        // 2) Кабинет менеджера — синяя карта
        ironDoor(6, 1, 11, Direction.EAST);
        reader(6, 1, 10, Direction.EAST);
        reader(6, 1, 12, Direction.EAST);

        // 3) Дверь хранилища — красная карта (или дрель в стене из зала)
        ironDoor(13, 1, 11, Direction.EAST);
        reader(13, 1, 10, Direction.EAST);
        reader(13, 1, 12, Direction.EAST);
        vaultDoor(16, 8);

        // Сундук кассира: зелёная карта + записка с кодом сейфа персонала
        chest(3, 1, 5, Direction.SOUTH,
                new ItemStack(ModItems.KEYCARD),
                ModItems.codeNote(codeA, "Сейф персонала"),
                cash(8));

        // Сейф персонала
        SafeBlockEntity safeA = safe(8, 1, 12, Direction.NORTH, codeA);
        addTo(safeA, cash(10));

        // Кабинет менеджера: сундук с запиской и сейф с золотом
        chest(4, 1, 10, Direction.SOUTH,
                ModItems.codeNote(codeB, "Сейф менеджера"),
                cash(6));
        SafeBlockEntity safeB = safe(2, 1, 12, Direction.NORTH, codeB);
        addTo(safeB, gold(2), new ItemStack(ModBlocks.PAINTING));

        // Хранилище: три сейфа с неизвестными кодами (нужна дрель), золото, деньги
        lootSafe(15, 1, 13, Direction.NORTH, false);
        SafeBlockEntity vaultSafe = safe(17, 1, 13, Direction.NORTH, randomCode());
        if (vaultSafe != null) {
            vaultSafe.fillRandom(5, r);
            vaultSafe.addLoot(gold(3), r);
        }
        lootSafe(19, 1, 13, Direction.NORTH, true);
        fill(18, 1, 10, 19, 1, 11, ModBlocks.GOLD_PILE.getDefaultState());
        set(19, 2, 10, ModBlocks.GOLD_PILE.getDefaultState());
        fill(14, 1, 10, 14, 2, 10, ModBlocks.MONEY_BLOCK.getDefaultState());
        set(15, 1, 10, ModBlocks.MONEY_BLOCK.getDefaultState());
        set(14, 1, 12, ModBlocks.MONEY_BLOCK.getDefaultState());
        chest(19, 1, 9, Direction.SOUTH, cash(64), cash(64), gold(8), new ItemStack(ModItems.JEWELRY, 6));

        // Кассы за стойкой
        cashRegister(5, 1, 4, Direction.NORTH);
        cashRegister(8, 1, 4, Direction.NORTH);
        cashRegister(12, 1, 4, Direction.NORTH);
        cashRegister(15, 1, 4, Direction.NORTH);

        // Картины, камеры, компьютер охраны
        painting(4, 3, 7, Direction.NORTH);
        painting(16, 3, 7, Direction.NORTH);
        painting(1, 3, 4, Direction.EAST);
        painting(19, 3, 4, Direction.WEST);
        painting(3, 3, 13, Direction.NORTH);
        camera(10, 4, 1, Direction.SOUTH);
        camera(1, 4, 7, Direction.EAST);
        camera(14, 4, 12, Direction.EAST);
        camera(5, 4, 9, Direction.SOUTH);
        computer(4, 1, 13, Direction.NORTH);

        // Освещение
        for (int x : new int[]{4, 10, 16}) {
            lamp(x, 4, 2);
            lamp(x, 4, 6);
        }
        lamp(9, 4, 11);
        lamp(3, 4, 11);
        hangingLantern(16, 4, 11);
    }

    // ------------------------------------------------------------------ BIG DOOR

    private void buildDoor() {
        BlockState brick = Blocks.STONE_BRICKS.getDefaultState();
        fill(-1, 1, -1, 7, 6, 1, AIR);
        fill(0, 0, -1, 6, 0, 1, Blocks.POLISHED_ANDESITE.getDefaultState());
        fill(0, 1, 0, 6, 5, 0, brick);

        // дверь 3x3 в проёме
        for (int x = 2; x <= 4; x++) {
            for (int y = 1; y <= 3; y++) {
                set(x, y, 0, ModBlocks.SECURITY_DOOR.getDefaultState().with(SecurityDoorBlock.FACING, Direction.NORTH));
            }
        }

        // управление: зелёный считыватель слева, кодовые панели справа с обеих сторон
        String code = randomCode();
        reader(1, 2, 0, Direction.NORTH);
        panel(5, 2, 0, Direction.NORTH, code);
        panel(5, 3, 0, Direction.SOUTH, code);

        chest(0, 1, -1, Direction.SOUTH,
                new ItemStack(ModItems.KEYCARD),
                ModItems.codeNote(code, "Кодовая панель двери"));
        lamp(3, 4, 0);
    }

    // ------------------------------------------------------------------ JEWELRY

    private void buildJewelry() {
        BlockState wall = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState glass = Blocks.GLASS.getDefaultState();

        fill(-1, 1, -1, 13, 7, 13, AIR);
        fill(0, 0, 0, 12, 0, 12, Blocks.POLISHED_ANDESITE.getDefaultState());
        fill(0, 1, 0, 12, 4, 12, wall);
        fill(1, 1, 1, 11, 4, 11, AIR);
        fill(0, 5, 0, 12, 5, 12, wall);

        // Витрина спереди и вход
        fill(2, 1, 0, 10, 3, 0, glass);
        fill(6, 1, 0, 6, 2, 0, AIR);

        // Прилавок
        fill(3, 1, 5, 9, 1, 5, Blocks.QUARTZ_BLOCK.getDefaultState());

        // Задняя комната
        fill(1, 1, 8, 11, 4, 8, wall);
        String codeS1 = randomCode();
        ironDoor(6, 1, 8, Direction.NORTH);
        reader(5, 1, 8, Direction.NORTH);
        reader(7, 1, 8, Direction.NORTH);
        vaultDoor(10, 8);

        // Витрины с украшениями: разбил — получил добычу
        int[][] cases = {{2, 2}, {2, 4}, {2, 6}, {10, 2}, {10, 4}, {10, 6}, {4, 2}, {8, 2}};
        for (int[] c : cases) {
            set(c[0], 1, c[1], ModBlocks.DISPLAY_CASE_FULL.getDefaultState());
        }

        // Касса: зелёная карта + записка с кодом
        chest(6, 1, 6, Direction.SOUTH,
                new ItemStack(ModItems.KEYCARD),
                ModItems.codeNote(codeS1, "Сейф в подсобке"),
                cash(10));

        // Сейфы
        SafeBlockEntity s1 = safe(2, 1, 11, Direction.NORTH, codeS1);
        addTo(s1, new ItemStack(ModItems.JEWELRY, 4), gold(2), cash(8));
        lootSafe(5, 1, 11, Direction.NORTH, true);
        lootSafe(9, 1, 11, Direction.NORTH, false);

        cashRegister(4, 1, 6, Direction.NORTH);
        cashRegister(8, 1, 6, Direction.NORTH);
        painting(3, 3, 7, Direction.NORTH);
        painting(9, 3, 7, Direction.NORTH);
        camera(6, 4, 1, Direction.SOUTH);
        camera(1, 4, 9, Direction.EAST);
        computer(11, 1, 9, Direction.WEST);

        lamp(3, 4, 3);
        lamp(9, 4, 3);
        lamp(6, 4, 6);
        lamp(3, 4, 10);
        lamp(9, 4, 10);
    }
}
