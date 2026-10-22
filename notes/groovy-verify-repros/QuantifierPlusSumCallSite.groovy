package clearsplit

import groovy.contracts.Ensures
import groovy.contracts.Requires
import groovy.transform.TypeChecked

@TypeChecked(extensions = 'verification.VerifyChecker')
class Lab {
    // C: left = 0, fresh shares
    @Requires({ joined != null && (0..<joined.length).every { joined[it] == 0 || joined[it] == 1 } })
    @Ensures({ result.length == joined.length })
    static int[] c(int[] joined) {
        int[] shares = new int[joined.length]
        Split.handOut(shares, joined, 0)
    }

    static boolean flags(int[] j) { true }

    // D: a callee with the same requires shape, no sums
    @Requires({ xs != null && (0..<xs.length).every { xs[it] == 0 || xs[it] == 1 } })
    static int d0(int[] xs) { 0 }

    @Requires({ joined != null && (0..<joined.length).every { joined[it] == 0 || joined[it] == 1 } })
    static int d(int[] joined) {
        d0(joined)
    }

    // E: callee needing a sum bound
    @Requires({ xs != null && 0 <= m && m <= xs.sum() })
    static int e0(int[] xs, int m) { 0 }

    @Requires({ joined != null && joined.sum() > 0 })
    static int e(int[] joined) {
        e0(joined, 0)
    }
}
