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
- **android**: an Android app in dynamic Groovy, built the way
  [groovy-android-tictactoe](../groovy-android-tictactoe) is: no Groovy Android plugin, just groovyc
  against `android.jar` and a stock Android Gradle Plugin.

All three apps share the one verified core, and everything runs on Groovy 6.0.0. Grails 8 is on Groovy 5.1, but groovy-verify needs Groovy 6,
so the web app uses Grails 9, which is.

## What's proved

| Where | Claim |
|---|---|
| `Split.equally(total, joined)` | the shares add up to `total` exactly; non-participants pay 0; each participant pays `total.intdiv(p)` or one cent more |
| `Balances.afterExpense` | each member's balance moves by exactly `(payer ? amount : 0) - share` (Dafny's `AddExpenseDelta`), and the total is unchanged when the shares add up to the amount |
| `Balances.afterSettlement` | `from` gains, `to` loses, everyone else is unchanged (`AddSettlementDelta`), and the total is unchanged |
| `Balances.of(...)` | **conservation**: folding `afterExpense` and `afterSettlement` over any valid history, the balances sum to zero |

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
groovy-verify checkout next to this one (`../groovy-verify`, or `-PgroovyVerifyDir=...`) at
`5e29afb` or later: the kernel's helper calls and its fold need fixes from there.
groovy-fx is extracted from the `groovy11434` branch of a local apache/groovy clone
(`-PgroovyRepo=...`, default `~/projects/groovy`), or cloned from GitHub when there is none.

```sh
./gradlew build            # verify the core, run all tests, build the war (and the APKs)
./gradlew :web:bootRun     # http://localhost:8080
./gradlew :fx:run          # the desktop app
```

Verification runs inside `:core:compileGroovy`; a failed proof is a compile error with a
counterexample where Z3 finds one. The FX test runs the UI headless (Monocle) and writes
`fx/build/snapshots/balances.png`.

## Android

The `android` modules join the build only when an Android SDK is found (`sdk.dir` in
`local.properties`, or `ANDROID_HOME`), so everything else builds without one.

- `android/ui`: `MainActivity`, the whole app, built in code in dynamic Groovy. The group is
  saved to preferences after every change and restored by replaying its history through `Group`,
  so a saved group passes the same validation as one entered by hand.
- `android/app`: the Android application module (AGP 9.4, minSdk 26, targetSdk 36). It supplies
  the `grooid` Groovy runtime and holds the R8 rules.
- `android/shim` and `android/management-shim`: EXPERIMENT ONLY stand-ins for JDK classes ART
  lacks (see below).

```sh
./gradlew :android:app:assembleDebug     # or assembleRelease: R8, about 1.1 MB, signed with the debug key
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n clearsplit.android/.MainActivity
```

Long-press "New group" to start over. `android/screenshots/` has the app on an API 36 emulator.

What came up:

- **groovy-contracts can't initialise on Android.** `Configurator` reads the JVM's `-ea`/`-da`
  flags through `java.lang.management.ManagementFactory`, which ART doesn't have, so the first
  contract check throws `NoClassDefFoundError` and the app crashes. `management-shim` supplies a
  stub that reports no flags, which leaves contracts enabled (their default). The fix belongs in
  groovy-contracts: fall back to the default when the management API is missing. It needs its
  own module because javac patches only one JDK module per compilation.
- `SwitchPoint` is still missing on ART, so the hello project's `shim` is still needed.
- `groovy-contracts` ships its own `META-INF/groovy-release-info.properties`, which collides with
  the grooid jar's when packaging, so the app picks the first.
