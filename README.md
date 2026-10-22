# ClearSplit in Groovy

A spike reimplementing ClearSplit, the verified expense splitter from
[Dafny](https://github.com/metareflection/dafny-replay/tree/main/clear-split) and
[LemmaScript](https://github.com/midspiral/clear-split-lemmascript), in Groovy:

- **core**: the splitting and balance logic, proved at compile time with
  [groovy-verify](../groovy-verify) (Z3 behind a `@TypeChecked` extension, contracts as stock
  `groovy.contracts` annotations).
- **web**: a Grails 9 (snapshot) web app.
- **fx**: a desktop app using the incubating `groovy-fx` module from
  [apache/groovy#2989](https://github.com/apache/groovy/pull/2989).

Everything runs on Groovy 6.0.0. Grails 8 is on Groovy 5.1, but groovy-verify needs Groovy 6,
so the web app uses Grails 9, which is.

## What's proved

| Where | Claim |
|---|---|
| `Split.equally(total, joined)` | the shares add up to `total` exactly; non-participants pay 0; each participant pays `total.intdiv(p)` or one cent more |
| `Balances.afterExpense` | each member's balance moves by exactly `(payer ? amount : 0) - share` (Dafny's `AddExpenseDelta`), and the total is unchanged when the shares add up to the amount |
| `Balances.afterSettlement` | `from` gains, `to` loses, everyone else is unchanged (`AddSettlementDelta`), and the total is unchanged |
| `Balances.of(...)` | **conservation**: after any valid history of expenses and settlements, the balances sum to zero |

Each proof was also checked for teeth: breaking the code or dropping a hypothesis makes the build
fail. For example, giving the leftover cent to the next member regardless of whether they joined
(what groovy-verify's earlier LemmaScript `allocate` port does, since its spec only asks for the
right sum) is rejected by `equally`'s stronger spec.

The contracts are ordinary Groovy, so they also run as runtime checks in both apps. `Group` is
the unverified, immutable model the apps use. Like Dafny's `Step`, it only accepts valid actions,
so the kernel's preconditions always hold.

Not ported: Dafny's string-keyed maps (this follows LemmaScript's member indices), the
`ExplainSumsToBalance` lemma (`Group.explain` is tested rather than proved), and the
multi-user collaboration modules.

`notes/groovy-verify-findings.md` lists what came up in groovy-verify along the way, including
one soundness bug, each with a repro.

## Building and running

Needs JDK 21+ (Gradle runs on 21, groovy-verify builds with a 25 toolchain), plus a
groovy-verify checkout next to this one (`../groovy-verify`, or `-PgroovyVerifyDir=...`).
groovy-fx is extracted from the `groovy11434` branch of a local apache/groovy clone
(`-PgroovyRepo=...`, default `~/projects/groovy`), or cloned from GitHub when there is none.

```sh
./gradlew build            # verify the core, run all tests, build the war
./gradlew :web:bootRun     # http://localhost:8080
./gradlew :fx:run          # the desktop app
```

Verification runs inside `:core:compileGroovy`; a failed proof is a compile error with a
counterexample where Z3 finds one. The FX test runs the UI headless (Monocle) and writes
`fx/build/snapshots/balances.png`.
