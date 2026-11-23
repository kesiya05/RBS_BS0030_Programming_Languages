// BS0030 Week 9 - Kotlin domain model (start of A5)
//
// Run from the repository root:
//   kotlinc labs/week09/work/Main.kt -include-runtime -d /tmp/week09.jar
//   java -jar /tmp/week09.jar

// ---------------------------------------------------------------------------
// Policies: interchangeable behaviour behind one interface.
// ---------------------------------------------------------------------------

interface LoanPolicy { fun limit(): Int }

class StandardPolicy : LoanPolicy {
    override fun limit() = 3
}

class ResearchPolicy : LoanPolicy {
    override fun limit() = 5
}

// ---------------------------------------------------------------------------
// Account
// ---------------------------------------------------------------------------
// INVARIANT: 0 <= activeLoans <= limit
//
// - activeLoans has a private setter, so only borrow() and returnOne() can
//   change it. Code outside the class can read it but not assign it.
// - The limit is read from the policy ONCE, in the constructor, and stored in
//   a val. So the limit stays constant for the account's whole lifetime, even
//   if a policy object were written to return different values later.
// - init rejects a negative limit before any Account object exists.
//
// REJECTION BEHAVIOUR: an operation that would break the invariant does not
// change anything and returns false. A successful operation returns true.
// (Week 10 replaces Boolean with richer outcome types.)

class Account(private val policy: LoanPolicy) {
    val limit: Int = policy.limit()

    var activeLoans: Int = 0
        private set

    init {
        require(limit >= 0) { "loan limit must be nonnegative, was $limit" }
    }

    // Accept only while below the limit: 0 <= activeLoans < limit -> +1 keeps
    // activeLoans <= limit.
    fun borrow(): Boolean {
        if (activeLoans >= limit) return false
        activeLoans++
        return true
    }

    // Reject when there is no loan to return, so activeLoans never goes below 0.
    fun returnOne(): Boolean {
        if (activeLoans == 0) return false
        activeLoans--
        return true
    }

    fun policyName(): String = policy::class.simpleName ?: "unknown"
}

// ---------------------------------------------------------------------------
// Extension: a pure state-transition version (kept next to the OOP one).
// The state is an immutable value; each transition returns a NEW state plus
// whether it was accepted. Nothing is mutated, so old states stay valid.
// ---------------------------------------------------------------------------

data class LoanState(val limit: Int, val activeLoans: Int = 0) {
    init {
        require(limit >= 0)
        require(activeLoans in 0..limit)
    }
}

data class Transition(val accepted: Boolean, val state: LoanState)

fun borrow(state: LoanState): Transition =
    if (state.activeLoans >= state.limit) Transition(false, state)
    else Transition(true, state.copy(activeLoans = state.activeLoans + 1))

fun returnOne(state: LoanState): Transition =
    if (state.activeLoans == 0) Transition(false, state)
    else Transition(true, state.copy(activeLoans = state.activeLoans - 1))

// ---------------------------------------------------------------------------
// Boundary checks, written so the same test runs for any policy.
// ---------------------------------------------------------------------------

fun checkBoundaries(account: Account) {
    val limit = account.limit
    val name = account.policyName()

    // 1. Empty return: rejected, count stays 0.
    check(!account.returnOne()) { "$name: empty return should be rejected" }
    check(account.activeLoans == 0)

    // 2. Borrow up to exactly the limit: every borrow accepted.
    repeat(limit) { i ->
        check(account.borrow()) { "$name: borrow ${i + 1} should be accepted" }
    }
    check(account.activeLoans == limit) { "$name: should be at the exact limit" }

    // 3. One extra borrow: rejected, count unchanged.
    check(!account.borrow()) { "$name: borrow past the limit should be rejected" }
    check(account.activeLoans == limit)

    // 4. Repeated return: accepted down to 0, then the next is rejected.
    repeat(limit) { i ->
        check(account.returnOne()) { "$name: return ${i + 1} should be accepted" }
    }
    check(account.activeLoans == 0)
    check(!account.returnOne()) { "$name: repeated return at 0 should be rejected" }
    check(account.activeLoans == 0)

    println("$name (limit $limit): empty return, exact limit, extra borrow, repeated return - OK")
}

fun main() {
    // Core: one policy.
    val account = Account(StandardPolicy())
    check(account.borrow())
    check(account.activeLoans == 1)
    check(account.returnOne())
    check(!account.returnOne())
    println("Core StandardPolicy borrow/return - OK")

    // Standard: two interchangeable policies through the same code.
    // checkBoundaries only knows the LoanPolicy interface. Which limit()
    // runs is decided at runtime by the actual object (dynamic dispatch).
    val policies: List<LoanPolicy> = listOf(StandardPolicy(), ResearchPolicy())
    for (policy in policies) {
        checkBoundaries(Account(policy))
    }

    // Constructor validation: a negative limit never produces an Account.
    val negative = object : LoanPolicy { override fun limit() = -1 }
    val rejected = runCatching { Account(negative) }
    check(rejected.isFailure)
    println("Negative limit rejected: ${rejected.exceptionOrNull()?.message}")

    // val does not freeze a mutable object.
    val shelf = mutableListOf("Algorithms")
    shelf.add("Poems")                       // allowed: the list object changes
    // shelf = mutableListOf("Databases")    // compile error: val cannot be reassigned
    check(shelf == listOf("Algorithms", "Poems"))

    val sameAccount = account                // val reference to the same Account
    sameAccount.borrow()                     // the object behind the val still changes
    check(account.activeLoans == 1)          // visible through both names
    println("val keeps the reference fixed, not the object: shelf=$shelf, activeLoans=${account.activeLoans}")

    // Extension: pure state-transition version.
    val s0 = LoanState(limit = 3)
    val t1 = borrow(s0)
    val t2 = borrow(t1.state)
    check(t1.accepted && t2.accepted && t2.state.activeLoans == 2)
    check(s0.activeLoans == 0)               // the old state is unchanged
    val full = (1..3).fold(s0) { s, _ -> borrow(s).state }
    check(!borrow(full).accepted && borrow(full).state == full)
    check(!returnOne(s0).accepted)
    println("Pure version: s0=$s0, after two borrows=${t2.state}, extra borrow at full rejected - OK")

    println("All checks passed.")
}
