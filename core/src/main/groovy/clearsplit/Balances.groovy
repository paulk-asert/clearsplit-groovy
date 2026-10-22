package clearsplit

import groovy.contracts.Decreases
import groovy.contracts.Ensures
import groovy.contracts.Invariant
import groovy.contracts.Requires
import groovy.transform.TypeChecked

/**
 * Verified balance updates. A balance is in cents: positive means the group owes that member,
 * negative means the member owes the group.
 */
@TypeChecked(extensions = 'verification.VerifyChecker')
class Balances {
    /**
     * The balances after an expense: the payer is owed the amount, and every member owes their
     * share (Dafny's AddExpenseDelta). When the shares add up to the amount, no money is created
     * or destroyed.
     */
    @Requires({ bal != null && shares != null && shares.length == bal.length && 0 <= payer && payer < bal.length })
    @Ensures({ result.length == bal.length })
    @Ensures({ (0..<bal.length).every { result[it] == bal[it] + (it == payer ? amount : 0) - shares[it] } })
    @Ensures({ shares.sum() == amount ==> result.sum() == bal.sum() })
    static int[] afterExpense(int[] bal, int payer, int amount, int[] shares) {
        int n = bal.length
        int[] result = new int[n]
        int r = 0
        int b = 0
        int s = 0
        int i = 0
        @Invariant({ 0 <= i && i <= n && n == bal.length && n == shares.length && result.length == n && 0 <= payer && payer < n })
        @Invariant({ r == result[0..<i].sum(0) && b == bal[0..<i].sum(0) && s == shares[0..<i].sum(0) })
        @Invariant({ r == b + (payer < i ? amount : 0) - s })
        @Invariant({ (0..<i).every { result[it] == bal[it] + (it == payer ? amount : 0) - shares[it] } })
        @Decreases({ n - i })
        while (i < n) {
            result[i] = bal[i] + (i == payer ? amount : 0) - shares[i]
            r = r + result[i]
            b = b + bal[i]
            s = s + shares[i]
            i = i + 1
        }
        return result
    }

    /**
     * The balances after member from pays member to: from is owed less, to owes less
     * (Dafny's AddSettlementDelta), and the total is unchanged.
     */
    @Requires({ bal != null && 0 <= from && from < bal.length && 0 <= to && to < bal.length && from != to })
    @Ensures({ result.length == bal.length && result.sum() == bal.sum() })
    @Ensures({ (0..<bal.length).every { result[it] == bal[it] + (it == from ? amount : 0) - (it == to ? amount : 0) } })
    static int[] afterSettlement(int[] bal, int from, int to, int amount) {
        int n = bal.length
        int[] result = new int[n]
        int r = 0
        int b = 0
        int i = 0
        @Invariant({ 0 <= i && i <= n && n == bal.length && result.length == n })
        @Invariant({ 0 <= from && from < n && 0 <= to && to < n && from != to })
        @Invariant({ r == result[0..<i].sum(0) && b == bal[0..<i].sum(0) })
        @Invariant({ r == b + (from < i ? amount : 0) - (to < i ? amount : 0) })
        @Invariant({ (0..<i).every { result[it] == bal[it] + (it == from ? amount : 0) - (it == to ? amount : 0) } })
        @Decreases({ n - i })
        while (i < n) {
            result[i] = bal[i] + (i == from ? amount : 0) - (i == to ? amount : 0)
            r = r + result[i]
            b = b + bal[i]
            i = i + 1
        }
        return result
    }

    /**
     * Everyone's balance after a history of expenses and then settlements, starting from zero:
     * ClearSplit's conservation theorem. Expense e was paid by payer[e], cost amount[e] and its
     * shares are flat[off[e] ..< off[e] + members]; settlement t paid paid[t] from from[t] to to[t].
     * When every expense's shares add up to its amount, the balances always sum to zero.
     */
    @Requires({ members >= 0 && payer != null && amount != null && off != null && flat != null })
    @Requires({ payer.length == amount.length && off.length == amount.length })
    @Requires({ (0..<payer.length).every { 0 <= payer[it] && payer[it] < members && 0 <= off[it] && off[it] + members <= flat.length } })
    @Requires({ (0..<payer.length).every { flat[off[it]..<off[it] + members].sum(0) == amount[it] } })
    @Requires({ from != null && to != null && paid != null && from.length == to.length && paid.length == to.length })
    @Requires({ (0..<from.length).every { 0 <= from[it] && from[it] < members && 0 <= to[it] && to[it] < members && from[it] != to[it] } })
    @Ensures({ result.length == members && result.sum() == 0 })
    static int[] of(int members, int[] payer, int[] amount, int[] flat, int[] off, int[] from, int[] to, int[] paid) {
        int[] bal = new int[members]
        assert bal.sum() == 0
        int e = 0
        @Invariant({ 0 <= e && e <= payer.length && payer.length == amount.length && off.length == amount.length })
        @Invariant({ bal.length == members && bal.sum() == 0 })
        @Invariant({ (0..<payer.length).every { 0 <= payer[it] && payer[it] < members && 0 <= off[it] && off[it] + members <= flat.length } })
        @Invariant({ (0..<payer.length).every { flat[off[it]..<off[it] + members].sum(0) == amount[it] } })
        @Decreases({ payer.length - e })
        while (e < payer.length) {
            bal[payer[e]] = bal[payer[e]] + amount[e]          // the payer is owed the amount
            int o = off[e]
            int s = 0
            int i = 0
            @Invariant({ 0 <= i && i <= members && bal.length == members && 0 <= o && o + members <= flat.length })
            @Invariant({ s == flat[o..<o + i].sum(0) && bal.sum() == amount[e] - s })
            @Decreases({ members - i })
            while (i < members) {
                bal[i] = bal[i] - flat[o + i]                  // and everyone owes their share
                s = s + flat[o + i]
                i = i + 1
            }
            e = e + 1
        }
        int t = 0
        @Invariant({ 0 <= t && t <= from.length && from.length == to.length && paid.length == to.length })
        @Invariant({ bal.length == members && bal.sum() == 0 })
        @Invariant({ (0..<from.length).every { 0 <= from[it] && from[it] < members && 0 <= to[it] && to[it] < members && from[it] != to[it] } })
        @Decreases({ from.length - t })
        while (t < from.length) {
            bal[from[t]] = bal[from[t]] + paid[t]
            bal[to[t]] = bal[to[t]] - paid[t]
            t = t + 1
        }
        return bal
    }
}
