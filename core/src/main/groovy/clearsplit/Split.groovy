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
        int p = joined.sum()
        int base = total.intdiv(p)
        int left = total - p * base             // the cents lost to flooring
        assert 0 <= left && left < p
        int[] shares = baseShares(base, joined)
        handOut(shares, joined, left)
    }

    /** Everyone taking part pays base, and nobody else pays anything. */
    @Requires({ joined != null && (0..<joined.length).every { joined[it] == 0 || joined[it] == 1 } })
    @Ensures({ result != null && result.length == joined.length && result.sum() == joined.sum() * base })
    @Ensures({ (0..<joined.length).every { result[it] == (joined[it] == 1 ? base : 0) } })
    static int[] baseShares(int base, int[] joined) {
        int n = joined.length
        int[] result = new int[n]
        int t = 0
        int c = 0
        int i = 0
        @Invariant({ 0 <= i && i <= n && n == joined.length && result.length == n })
        @Invariant({ t == result[0..<i].sum(0) && c == joined[0..<i].sum(0) && t == c * base })
        @Invariant({ (0..<i).every { result[it] == (joined[it] == 1 ? base : 0) } })
        @Decreases({ n - i })
        while (i < n) {
            result[i] = joined[i] == 1 ? base : 0
            t = t + result[i]
            c = c + joined[i]
            i = i + 1
        }
        return result
    }

    /** The shares again, with one cent more for each of the first left participants. */
    @Requires({ shares != null && joined != null && shares.length == joined.length })
    @Requires({ (0..<joined.length).every { joined[it] == 0 || joined[it] == 1 } && 0 <= left && left <= joined.sum() })
    @Ensures({ result != null && result.length == shares.length && result.sum() == shares.sum() + left })
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
}
