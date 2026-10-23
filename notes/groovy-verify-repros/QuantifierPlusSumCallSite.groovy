package clearsplit

import groovy.contracts.Decreases
import groovy.contracts.Ensures
import groovy.contracts.Invariant
import groovy.contracts.Requires
import groovy.transform.TypeChecked

@TypeChecked(extensions = 'verification.VerifyChecker')
class Lab {
    // The callee: the original Split.handOut, before equally became two loops in one method
    @Requires({ shares != null && joined != null && shares.length == joined.length })
    @Requires({ (0..<joined.length).every { joined[it] == 0 || joined[it] == 1 } && 0 <= left && left <= joined.sum() })
    @Ensures({ result.length == shares.length && result.sum() == shares.sum() + left })
    @Ensures({ (0..<shares.length).every { result[it] == shares[it] || (joined[it] == 1 && result[it] == shares[it] + 1) } })
    static int[] handOut(int[] shares, int[] joined, int left) {
        int n = shares.length
        int[] result = new int[n]
        int given = 0
        int r = 0
        int s = 0
        int c = 0
        int k = 0
        @Invariant({ 0 <= k && k <= n && n == shares.length && n == joined.length && result.length == n })
        @Invariant({ r == result[0..<k].sum(0) && s == shares[0..<k].sum(0) && c == joined[0..<k].sum(0) })
        @Invariant({ given == (c < left ? c : left) && r == s + given })
        @Invariant({ (0..<k).every { result[it] == shares[it] || (joined[it] == 1 && result[it] == shares[it] + 1) } })
        @Decreases({ n - k })
        while (k < n) {
            if (given < left && joined[k] == 1) {
                result[k] = shares[k] + 1
                given = given + 1
            } else {
                result[k] = shares[k]
            }
            r = r + result[k]
            s = s + shares[k]
            c = c + joined[k]
            k = k + 1
        }
        return result
    }

    // C: left = 0, fresh shares
    @Requires({ joined != null && (0..<joined.length).every { joined[it] == 0 || joined[it] == 1 } })
    @Ensures({ result.length == joined.length })
    static int[] c(int[] joined) {
        int[] shares = new int[joined.length]
        handOut(shares, joined, 0)
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
