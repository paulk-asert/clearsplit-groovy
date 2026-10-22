package clearsplit

import groovy.transform.CompileStatic

/** An expense: payer paid amount cents, of which member m consumed shares[m]. */
@CompileStatic
record Expense(String description, int payer, int amount, List<Integer> shares) implements Serializable {}

/** A settlement: member from paid member to amount cents. */
@CompileStatic
record Settlement(int from, int to, int amount) implements Serializable {}

/**
 * A group sharing expenses: the app-facing model over the verified kernel ({@link Split},
 * {@link Balances}). It's immutable, and like Dafny ClearSplit's Step it only lets valid actions
 * in, so the kernel's preconditions, and with them the conservation theorem, always apply.
 */
@CompileStatic
record Group(List<String> members, List<Expense> expenses, List<Settlement> settlements) implements Serializable {

    static Group of(List<String> names) {
        def members = names*.trim().findAll()
        if (members.size() < 2) throw new IllegalArgumentException('A group needs at least two members')
        if (members.toUnique().size() != members.size()) throw new IllegalArgumentException('Member names must be distinct')
        new Group(List.copyOf(members), [], [])
    }

    int size() { members.size() }

    /** An expense split equally among the participants, by the verified {@link Split#equally}. */
    Expense splitEqually(String description, int payer, int amount, Collection<Integer> participants) {
        if (amount < 0) throw new IllegalArgumentException('The amount cannot be negative')
        int[] joined = new int[size()]
        participants.each { int m -> if (0 <= m && m < size()) joined[m] = 1 }
        if (joined.sum() == 0) throw new IllegalArgumentException('Choose at least one member to share the expense')
        new Expense(description, payer, amount, Split.equally(amount, joined).toList())
    }

    /** This group with the expense added; an invalid expense is rejected. */
    Group plus(Expense e) {
        if (!(0 <= e.payer() && e.payer() < size())) throw new IllegalArgumentException('The payer must be a member')
        if (e.amount() < 0) throw new IllegalArgumentException('The amount cannot be negative')
        if (e.shares().size() != size() || e.shares().any { int s -> s < 0 }) throw new IllegalArgumentException('Every member needs a share of zero or more')
        if (e.shares().sum(0) != e.amount()) throw new IllegalArgumentException('The shares must add up to the amount')
        new Group(members, expenses + e, settlements)
    }

    /** This group with the settlement added; an invalid settlement is rejected. */
    Group plus(Settlement s) {
        if (!(0 <= s.from() && s.from() < size() && 0 <= s.to() && s.to() < size())) throw new IllegalArgumentException('Both sides must be members')
        if (s.from() == s.to()) throw new IllegalArgumentException('A member cannot pay themselves')
        if (s.amount() < 0) throw new IllegalArgumentException('The amount cannot be negative')
        new Group(members, expenses, settlements + s)
    }

    /** Everyone's balance, by the verified {@link Balances#of}: they always sum to zero. */
    int[] balances() {
        int[] flat = expenses.collectMany { it.shares() } as int[]
        int[] off = (0..<expenses.size()).collect { int e -> e * size() } as int[]
        Balances.of(size(),
            expenses*.payer() as int[], expenses*.amount() as int[], flat, off,
            settlements*.from() as int[], settlements*.to() as int[], settlements*.amount() as int[])
    }

    /** Member m's balance change from each expense then each settlement: the history explaining their balance. */
    List<Integer> explain(int m) {
        expenses.collect { Expense e -> (e.payer() == m ? e.amount() : 0) - e.shares()[m] } +
            settlements.collect { Settlement s -> (s.from() == m ? s.amount() : 0) - (s.to() == m ? s.amount() : 0) }
    }

    int totalSpent() { (int) expenses*.amount().sum(0) }

    /** Formats cents as dollars, e.g. -1234 as -$12.34. */
    static String money(int cents) {
        String.format('%s$%d.%02d', cents < 0 ? '-' : '', cents.abs().intdiv(100), cents.abs() % 100)
    }

    /** Parses dollars to cents, e.g. '12.3' as 1230; null when it isn't an amount. */
    static Integer cents(String dollars) {
        try {
            def value = new BigDecimal(dollars?.trim() ?: '')
            value.signum() < 0 || value.scale() > 2 ? null : value.movePointRight(2).intValueExact()
        } catch (NumberFormatException | ArithmeticException ignored) {
            null
        }
    }
}
