package java.lang.invoke;

/**
 * Experiment-only stand-in for the JDK class ART lacks. Same contract: a guard that answers
 * true until invalidated, expressed as a MutableCallSite so linked guardWithTest chains observe
 * the flip. On a JVM this class is never loaded (java.base wins); on Android the app's DEX
 * supplies it.
 */
public class SwitchPoint {
    private static final MethodHandle K_TRUE = MethodHandles.constant(boolean.class, true);
    private static final MethodHandle K_FALSE = MethodHandles.constant(boolean.class, false);

    private final MutableCallSite mcs = new MutableCallSite(K_TRUE);
    private final MethodHandle mcsInvoker = mcs.dynamicInvoker();

    public SwitchPoint() {
    }

    public boolean hasBeenInvalidated() {
        return mcs.getTarget() != K_TRUE;
    }

    public MethodHandle guardWithTest(MethodHandle target, MethodHandle fallback) {
        if (mcs.getTarget() == K_FALSE) {
            return fallback;
        }
        return MethodHandles.guardWithTest(mcsInvoker, target, fallback);
    }

    public static void invalidateAll(SwitchPoint[] switchPoints) {
        if (switchPoints.length == 0) {
            return;
        }
        // ART has no MutableCallSite.syncAll either; setTarget alone has to do.
        for (SwitchPoint sp : switchPoints) {
            if (sp == null) {
                break;
            }
            sp.mcs.setTarget(K_FALSE);
        }
    }
}
