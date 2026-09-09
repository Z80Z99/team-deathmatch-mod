package cn.blockforge.generated.generatedmod.map;

import java.util.function.BooleanSupplier;

/** Only the trusted snapshot writer may restore blocks outside the playable boundary. */
public final class ResetWriteAccess {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private ResetWriteAccess() { }
    public static boolean active() { return ACTIVE.get(); }
    public static boolean run(BooleanSupplier write) {
        boolean previous = ACTIVE.get();
        ACTIVE.set(true);
        try { return write.getAsBoolean(); } finally { ACTIVE.set(previous); }
    }
}
