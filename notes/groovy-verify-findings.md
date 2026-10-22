# groovy-verify findings from the ClearSplit port

Found while proving the ClearSplit kernel, last checked against groovy-verify `ab2ac99`. Each repro
in `groovy-verify-repros/` is a standalone class: drop it into `core/src/main/groovy/clearsplit/`
and compile.

## 1. Soundness: an array call result re-assigned inside a loop keeps its old facts

`ArrayCallInLoopUnsound.groovy`, method `m4`. `bump`'s contract says each call adds one to the
sum (`result.sum() == a.sum() + 1`), yet the loop invariant `v.sum() == 0` is reported preserved
across `v = bump(v)`. Outside a loop (`m5`) the same call is handled correctly.

It surfaced as a vacuous proof. ClearSplit's conservation theorem, written as a fold
`bal = afterExpenseRow(bal, …)`, still verified with its key hypothesis (each expense's shares add
up to its amount) deleted. The kernel now folds in place without calls, and the mutation is
correctly rejected.

## 2. Completeness: an array field passed as an argument loses its length

`FieldArgLength.groovy`. Under `@Invariant({ data.length == n })`, a call `bump(data, i)` with
`@Requires({ i < a.length })` is refuted with a false counterexample (`fails on: direct(0)`). The
callee's `a.size()` isn't tied to the field's `length`. Passing `this.data` or a local alias fails
the same way. ClearSplit's original mutable `Ledger` (class invariant `balances.sum() == 0`)
couldn't call its pure step functions because of this.

## 3. Completeness: a quantifier plus a sum in one callee precondition times out

`QuantifierPlusSumCallSite.groovy`. A callee requiring `(0..<xs.length).every { xs[it] == 0 || xs[it] == 1 }`
discharges, and one requiring `m <= xs.sum()` discharges, but `handOut`, requiring both, times out
at a call site whose caller assumes exactly the same quantifier. The same mix proves fine as a
loop invariant. This is why `Split.equally` is two loops in one method rather than two contracted
helpers.

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
