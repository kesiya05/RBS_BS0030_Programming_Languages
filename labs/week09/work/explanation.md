# Week 9 - Kotlin domain model (start of A5)

## Files

| File | Purpose |
|---|---|
| `Main.kt` | `LoanPolicy`, `StandardPolicy`, `ResearchPolicy`, `Account`, boundary checks, pure extension |
| `output.txt` | Observed output of the program |

## How to run

From the repository root (BS0030 Kotlin Codespace):

```bash
kotlinc labs/week09/work/Main.kt -include-runtime -d /tmp/week09.jar
java -jar /tmp/week09.jar
```

The jar goes to `/tmp` and is not committed. Every test uses `check(...)`, so a broken rule stops the program with an `IllegalStateException` instead of printing `All checks passed.`

## Invariant and rejection behaviour

The invariant is `0 <= activeLoans <= limit`.

- **Private setter.** `activeLoans` has a `private set`, so code outside `Account` can read it but cannot assign it. The only way to change it is through `borrow()` and `returnOne()`, which each check the rule first.
- **`borrow()`** is accepted only while `activeLoans < limit`, so after `+1` the count is still at most `limit`.
- **`returnOne()`** is accepted only while `activeLoans > 0`, so after `-1` the count is never negative.
- **Rejection.** A rejected operation changes nothing and returns `false`. A successful one returns `true`. Week 10 replaces the Boolean with richer outcome types.
- **Constructor validation.** `init { require(limit >= 0) }` throws before an `Account` with a negative limit can exist, so every account starts in a valid state (`activeLoans = 0`).
- **Constant limit.** The limit is read from the policy once, in the constructor, and stored in a `val`. Even if some policy object returned different values later, an existing account keeps the limit it started with.

## Boundary tests (observed)

`checkBoundaries` runs the same test on an account with each policy:

| Test | StandardPolicy (3) | ResearchPolicy (5) |
|---|---|---|
| Return with no loans | rejected, count stays 0 | rejected, count stays 0 |
| Borrow up to the exact limit | 3 accepted | 5 accepted |
| One extra borrow | rejected, count stays 3 | rejected, count stays 5 |
| Return everything, then once more | accepted down to 0, then rejected | same |

A policy with limit `-1` is rejected by `require` with the message `loan limit must be nonnegative, was -1`.

## Interface dispatch and composition

`Account` does not inherit from a policy. It **has** a policy (`private val policy: LoanPolicy`). This is composition: the account owns the loan-counting rules, and the policy only answers one question, "what is the limit?"

`checkBoundaries` and `Account` only know the `LoanPolicy` interface. When `policy.limit()` is called, which version runs is decided at runtime by the actual object: `StandardPolicy` returns 3 and `ResearchPolicy` returns 5. This is dynamic (interface) dispatch. That is why adding `ResearchPolicy` needed no change to `Account` or to the tests. They are interchangeable, and a new policy is just a new class.

## val does not freeze a mutable object

```kotlin
val shelf = mutableListOf("Algorithms")
shelf.add("Poems")                     // allowed
// shelf = mutableListOf("Databases")  // compile error
```

`val` fixes the **reference**: `shelf` always points to the same list. The list object itself is still mutable, so `add` works. The same happens with `val sameAccount = account`. `sameAccount.borrow()` changes the one shared `Account` object, and the change is visible through both names. To get a list that really cannot change, the type has to be read-only (`listOf`/`List`) or the data has to be copied.

## Extension: pure state-transition version

`LoanState` is an immutable `data class`, and its `init` block checks the same invariant. `borrow(state)` and `returnOne(state)` never change their input. They return a `Transition(accepted, newState)`, building the new state with `copy(...)`. When an operation is rejected, they return the same state unchanged.

| | OOP `Account` | Pure `LoanState` |
|---|---|---|
| State | hidden inside a mutable object | an immutable value passed in and out |
| Protecting the invariant | private setter plus checks in methods | `init` check, and only valid states can be built |
| Old states | overwritten | still available (`s0` keeps `activeLoans = 0`) |
| Testing | create an object, call methods, read the field | call a function and compare values |

Both versions are kept in `Main.kt`. The OOP one is the A5 baseline that Week 10 refines.
