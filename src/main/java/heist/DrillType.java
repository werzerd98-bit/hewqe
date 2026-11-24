package heist;

/** Характеристики бура. vaultTicks = 0 — дверь хранилища этим буром не взять. */
public enum DrillType {
    /** Обычная дрель: только сейфы, медленно, заклинивает чаще. */
    DRILL(45 * 20, 0, 900),
    /** Термобур: сейфы за 20 секунд, дверь хранилища за 60, заклинивает реже. */
    THERMAL(20 * 20, 60 * 20, 1500);

    public final int safeTicks;
    public final int vaultTicks;
    /** Шанс заклинивания: 1 к jamOdds на каждый тик работы. */
    public final int jamOdds;

    DrillType(int safeTicks, int vaultTicks, int jamOdds) {
        this.safeTicks = safeTicks;
        this.vaultTicks = vaultTicks;
        this.jamOdds = jamOdds;
    }
}
