# groovy-verify findings from the ClearSplit port

Found while proving the ClearSplit kernel, checked against groovy-verify `ab2ac99`, and re-tested against `bf7b4eb` (items 1 and 2) `b0c7c4d` (item 3) and `5e29afb` (item 4). Each repro
in `groovy-verify-repros/` is a standalone class: drop it into `core/src/main/groovy/clearsplit/`
and compile.

## 1. Soundness: an array call result re-assigned inside a loop keeps its old facts

`ArrayCallInLoopUnsound.groovy`, method `m4`. `bump`'s contract says each call adds one to the
sum (`result.sum() == a.sum() + 1`), yet the loop invariant `v.sum() == 0` is reported preserved
across `v = bump(v)`. Outside a loop (`m5`) the same call is handled correctly.

It surfaced as a vacuous proof. ClearSplit's conservation theorem, written as a fold
`bal = afterExpenseRow(bal, …)`, still verified with its key hypothesis (each expense's shares add
up to its amount) deleted. The kernel was rewritten to fold in place without calls, which
rejected the mutation correctly, until the fixes below let it go back to the fold.

**Fixed in groovy-verify `fb5ce5f`** ("a re-assigned array holds the new array"). Re-tested there:
`m4` and `m5` are now reported (undecided, as `Could not decide … (solver: timeout)`, since the
`.sum()` axioms are beyond the solver), and the rebuilt fold no longer verifies with its key
hypothesis deleted. The fold with the hypothesis kept was also undecided there.

**Completed in `bf7b4eb`** ("a sum fold over a call proves"). It fixed two causes: `new int[m]`'s
zero sum needed induction, and a call's `result` clashed with a callee formal of the same name.
`FoldConservation.groovy` now behaves as it should: `afterExpenseRow` and `foldWith` prove, while
`foldWithout` (key hypothesis deleted) is reported rather than proved. The verified core is
unchanged there. `Balances.of` is now the fold again, over `afterExpense` and `afterSettlement`,
and still rejects the mutation.

The clash was itself a soundness bug: when a contract related `result` to a same-named formal
(`xs = grow(xs)`, or a local named like the formal), the assumption became a contradiction and
everything after the call proved. It affected lists since groovy-verify Phase 225, and arrays from
`fb5ce5f`. The ClearSplit core never had that shape: none of its verified methods calls another
contracted method.

## 2. Completeness: an array field passed as an argument loses its length

`FieldArgLength.groovy`. Under `@Invariant({ data.length == n })`, a call `bump(data, i)` with
`@Requires({ i < a.length })` is refuted with a false counterexample (`fails on: direct(0)`). The
callee's `a.size()` isn't tied to the field's `length`. Passing `this.data` or a local alias fails
the same way. ClearSplit's original mutable `Ledger` (class invariant `balances.sum() == 0`)
couldn't call its pure step functions because of this.

**Fixed in `768fe5f`** (class invariants at call sites, `this.f` arguments). Re-tested at
`bf7b4eb`: all three calls now discharge `bump`'s precondition. Because `data` is re-assigned from
the call, the exit invariant's `data != null` then needs `bump` to promise
`result != null`; with that in its `@Ensures`, the repro compiles clean.

## 3. Completeness: a quantifier plus a sum in one callee precondition times out

`QuantifierPlusSumCallSite.groovy`. A callee requiring `(0..<xs.length).every { xs[it] == 0 || xs[it] == 1 }`
discharges, and one requiring `m <= xs.sum()` discharges, but `handOut`, requiring both, times out
at a call site whose caller assumes exactly the same quantifier. The same mix proves fine as a
loop invariant. This is why `Split.equally` was, for a while, two loops in one method rather than
two contracted helpers.

The repro is standalone (it includes the original `handOut`). At `bf7b4eb`, `handOut` itself
proved, and only the call in `c` failed: `Could not decide precondition of handOut at this call
site (… incomplete quantifiers)`. The cause was that no element bound ever reached a sum:
`joined.sum() >= 0` from the 0/1 quantifier needs induction.

**Fixed in `b0c7c4d`** ("an element bound bounds the sum"). An `every` that bounds each element by
constants now bounds every sum over a range of the array. Re-tested there: the repro compiles
clean. It still has teeth: calling `handOut(shares, joined, 1)` needs `1 <= joined.sum()`, which
the 0/1 bound doesn't give, and that call is reported, not proved.
`Split.equally` is back to calling `baseShares` and `handOut`.

## 4. Completeness: a call's precondition times out where the same assert proves

`RowThenCall.groovy`. The fold first copies each expense's shares out of the flattened array
with a contracted `row(flat, off[e], members)`, then calls `afterExpense(bal, p, amount[e], owed)`.
An `assert` stating that call's whole precondition, just before it, proves. The call itself is
`Could not decide precondition of afterExpense at this call site (solver: timeout)`. Renaming the
locals, the caller's parameters, or dropping `afterExpense`'s quantified postcondition made no
difference. Having `afterExpense` read its shares from `flat` at an offset instead (no `row`)
proved, so `Balances.of` used that shape for a while.

**Fixed in `5e29afb`** ("a call in a loop sees the calls before it"). A call's precondition inside
an annotated loop was checked by replaying the loop body up to the call, without applying the
contracts of earlier calls, so `owed` knew nothing. Re-tested there: the repro compiles clean, and
`Balances.of` is back to `row` plus `afterExpense(bal, payer[e], amount[e], shares)`, with no
stepping-stone assert.

## 5. Flaky: a call precondition timed out intermittently at `01c8f8d`

After groovy-verify's range rework (`01c8f8d`), `equally`'s call to `handOut` timed out in 3 of 8
clean compiles, against none at `b0c7c4d`. The folded range bound had become `joined.length + 0`,
a different term from the facts' `joined.length`. Fixed in `5e29afb`: 5 of 5 clean compiles pass,
at about 46 s each.

## Smaller observations

- `old` can't be used in a loop `@Invariant`, so a loop can't state an in-place update's effect
  against the entry state. The kernel builds a fresh array instead.
- `field = freshArray` after a loop that writes `freshArray` is a loud aliasing skip, as documented.
- Nested call arguments (`f(g(x))`) skip loudly; binding `g(x)` to a local first works.
- `int[][]` is outside the fragment. Flattened rows with explicit offsets work well, including
  slice sums over `flat[off..<off + n]`.
- Recursive pure helpers (`count(joined, k)`) inside loop invariants that also carry slice sums
  tend to time out. Counting 0/1 flags with a built-in slice sum (`joined[0..<k].sum(0)`) proves
  quickly.
- Stepping-stone `assert`s between loops (`assert c == p && t == shares.sum()`) turned several
  establishment timeouts into quick proofs.
- `-Dverify.z3.timeoutMs=8000` (CI's setting) didn't change any of the timeouts above. Restructuring
  did.
