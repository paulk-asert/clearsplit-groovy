package clearsplit

import groovy.contracts.Decreases
import groovy.contracts.Ensures
import groovy.contracts.Invariant
import groovy.contracts.Requires
import groovy.transform.TypeChecked

/**
 * Verified splitting of an amount (in cents) among the members taking part in an expense.
 * Participation is a 0/1 flag per member: joined[m] == 1 when member m shares the cost.
 */
@TypeChecked(extensions = 'verification.VerifyChecker')
class Split {
    /**
     * Splits total equally among the participants: each pays total.intdiv(p) or one cent more,
     * the shares add up to total exactly, and nobody outside the split pays anything.
     */
    @Requires({ joined != null && (0..<joined.length).every { joined[it] == 0 || joined[it] == 1 } })
    @Requires({ total >= 0 && joined.sum() > 0 })
    @Ensures({ result.length == joined.length && result.sum() == total })
    @Ensures({ (0..<joined.length).every { joined[it] == 0 ==> result[it] == 0 } })
    @Ensures({ (0..<joined.length).every { joined[it] == 1 ==>
        (result[it] == total.intdiv(joined.sum()) || result[it] == total.intdiv(joined.sum()) + 1) } })
    static int[] equally(int total, int[] joined) {
        int n = joined.length
        int p = joined.sum()
        int base = total.intdiv(p)
        int left = total - p * base             // the cents lost to flooring
        assert 0 <= left && left < p

        // everyone taking part pays base
        int[] shares = new int[n]
        int t = 0
        int c = 0
        int i = 0
        @Invariant({ 0 <= i && i <= n && n == joined.length && shares.length == n && p == joined.sum() })
        @Invariant({ t == shares[0..<i].sum(0) && c == joined[0..<i].sum(0) && t == c * base })
        @Invariant({ (0..<i).every { shares[it] == (joined[it] == 1 ? base : 0) } })
        @Decreases({ n - i })
        while (i < n) {
            shares[i] = joined[i] == 1 ? base : 0
            t = t + shares[i]
            c = c + joined[i]
            i = i + 1
        }
        assert c == p && t == shares.sum()
        assert shares.sum() == p * base

        // and the first `left` of them a cent more
        int[] result = new int[n]
        int given = 0
        int r = 0
        int s = 0
        c = 0
        int k = 0
        @Invariant({ 0 <= k && k <= n && n == joined.length && shares.length == n && result.length == n })
        @Invariant({ p == joined.sum() && shares.sum() == p * base && base == total.intdiv(p) && 0 <= left && left < p && total == p * base + left })
        @Invariant({ r == result[0..<k].sum(0) && s == shares[0..<k].sum(0) && c == joined[0..<k].sum(0) })
        @Invariant({ given == (c < left ? c : left) && r == s + given })
        @Invariant({ (0..<n).every { shares[it] == (joined[it] == 1 ? base : 0) } })
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
}
