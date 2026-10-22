package java.lang.management;

import java.util.Collections;
import java.util.List;

/**
 * EXPERIMENT ONLY: Android has no java.lang.management, but groovy-contracts' Configurator reads
 * the JVM's -ea/-da flags through it when the first contract runs. ART takes no such flags, so
 * this reports none and contracts stay enabled, their default. The real fix belongs in
 * groovy-contracts (fall back to the default when the management API is missing).
 */
public final class ManagementFactory {
    private static final RuntimeMXBean RUNTIME = new RuntimeMXBean() {
        @Override
        public List<String> getInputArguments() {
            return Collections.emptyList();
        }
    };

    private ManagementFactory() {
    }

    public static RuntimeMXBean getRuntimeMXBean() {
        return RUNTIME;
    }
}
