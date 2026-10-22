package clearsplit

import groovy.contracts.Decreases
import groovy.contracts.Ensures
import groovy.contracts.Invariant
import groovy.contracts.Requires
import groovy.transform.TypeChecked

@TypeChecked(extensions = 'verification.VerifyChecker')
class Lab {
    @Requires({ a != null && a.length > 0 })
    @Ensures({ result.length == a.length && result.sum() == a.sum() + 1 })
    static int[] bump(int[] a) {
        int[] r = new int[a.length]
        r[0] = 1
        r                    // deliberately wrong: the callee's own proof should fail
    }

    // M4 should FAIL: each call adds one to the sum, per bump's (assumed) contract
    @Requires({ n >= 1 && m > 0 })
    @Ensures({ result.sum() == 0 })
    static int[] m4(int n, int m) {
        int[] v = new int[m]
        assert v.sum() == 0
        int i = 0
        @Invariant({ 0 <= i && i <= n && v != null && v.length == m && v.sum() == 0 })
        @Decreases({ n - i })
        while (i < n) {
            v = bump(v)
            i = i + 1
        }
        return v
    }

    // M5 should FAIL: same without the loop
    @Requires({ m > 0 })
    @Ensures({ result.sum() == 0 })
    static int[] m5(int m) {
        int[] v = new int[m]
        assert v.sum() == 0
        v = bump(v)
        return v
    }
}
