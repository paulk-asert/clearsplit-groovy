package java.lang.management;

import java.util.List;

/** EXPERIMENT ONLY: the one method groovy-contracts uses; see {@link ManagementFactory}. */
public interface RuntimeMXBean {
    List<String> getInputArguments();
}
