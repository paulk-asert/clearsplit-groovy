package clearsplit

import org.junit.jupiter.api.Test
import org.apache.groovy.contracts.PreconditionViolation

import static org.junit.jupiter.api.Assertions.assertThrows

class ClearSplitTest {
    @Test
    void 'equal split hands the leftover cents to the first participants only'() {
        assert Split.equally(1000, [1, 1, 1] as int[]) == [334, 333, 333] as int[]
        assert Split.equally(1, [0, 1, 1] as int[]) == [0, 1, 0] as int[]   // allocate would charge member 0
        assert Split.equally(0, [1, 0] as int[]) == [0, 0] as int[]
    }

    @Test
    void 'equal split holds its contract over many random inputs'() {
        def rnd = new Random(42)
        1000.times {
            int[] joined = (0..<rnd.nextInt(1, 8)).collect { rnd.nextInt(2) } as int[]
            if (joined.sum() == 0) joined[0] = 1
            int total = rnd.nextInt(100_000)
            int[] shares = Split.equally(total, joined)
            int base = total.intdiv(joined.sum())
            assert shares.sum() == total
            joined.eachWithIndex { int j, int m -> assert j ? shares[m] in [base, base + 1] : shares[m] == 0 }
        }
    }

    @Test
    void 'the kernel contracts still run as runtime checks'() {
        assertThrows(PreconditionViolation) { Split.equally(100, [0, 0] as int[]) }
        assertThrows(PreconditionViolation) { Balances.afterSettlement([0, 0] as int[], 1, 1, 5) }
    }

    @Test
    void 'balances follow the delta laws and sum to zero'() {
        def g = Group.of(['Alice', 'Bob', 'Carol'])
        g += g.splitEqually('Dinner', 0, 9000, [0, 1, 2])
        g += g.splitEqually('Taxi', 1, 1000, [1, 2])
        g += new Settlement(2, 0, 2500)
        // Alice: +9000 - 3000 - 2500, Bob: -3000 + 1000 - 500, Carol: -3000 - 500 + 2500
        assert g.balances() == [3500, -2500, -1000] as int[]
        assert g.balances().sum() == 0
        (0..<g.size()).each { m -> assert g.explain(m).sum() == g.balances()[m] }
    }

    @Test
    void 'invalid actions are rejected'() {
        def g = Group.of(['Alice', 'Bob'])
        assertThrows(IllegalArgumentException) { g + new Expense('Bad', 0, 100, [60, 50]) }
        assertThrows(IllegalArgumentException) { g + new Settlement(1, 1, 5) }
        assertThrows(IllegalArgumentException) { Group.of(['Alice']) }
        assertThrows(IllegalArgumentException) { Group.of(['Alice', 'Alice']) }
    }

    @Test
    void 'money round-trips'() {
        assert Group.money(-1234) == '-$12.34'
        assert Group.money(5) == '$0.05'
        assert Group.cents('12.3') == 1230
        assert Group.cents('-1') == null && Group.cents('1.234') == null && Group.cents('x') == null
    }
}
