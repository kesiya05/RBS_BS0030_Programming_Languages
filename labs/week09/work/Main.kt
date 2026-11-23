// Week 9 lab - loan account with policies
// run from repo root:
// kotlinc labs/week09/work/Main.kt -include-runtime -d /tmp/week09.jar
// java -jar /tmp/week09.jar

interface LoanPolicy { fun limit(): Int }

class StandardPolicy : LoanPolicy {
    override fun limit() = 3
}

class ResearchPolicy : LoanPolicy {
    override fun limit() = 5
}

// invariant: 0 <= activeLoans <= limit
// activeLoans can only be changed inside the class (private set).
// if an operation would break the invariant it returns false and nothing changes.
class Account(private val policy: LoanPolicy) {
    // read the limit once so it can't change while the account exists
    val limit: Int = policy.limit()

    var activeLoans: Int = 0
        private set

    init {
        require(limit >= 0) { "loan limit must be nonnegative, was $limit" }
    }

    fun borrow(): Boolean {
        if (activeLoans >= limit) return false
        activeLoans++
        return true
    }

    fun returnOne(): Boolean {
        if (activeLoans == 0) return false
        activeLoans--
        return true
    }

    fun policyName(): String = policy::class.simpleName ?: "unknown"
}

// extension: same rules but without mutation.
// every call gives back a new state and the old one stays the same.
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

// same boundary tests for any policy
fun checkBoundaries(account: Account) {
    val limit = account.limit
    val name = account.policyName()

    // return when nothing is borrowed
    check(!account.returnOne()) { "$name: empty return should fail" }
    check(account.activeLoans == 0)

    // borrow up to the limit
    repeat(limit) { i ->
        check(account.borrow()) { "$name: borrow ${i + 1} should work" }
    }
    check(account.activeLoans == limit)

    // one more than allowed
    check(!account.borrow()) { "$name: borrow over the limit should fail" }
    check(account.activeLoans == limit)

    // return everything, then try one more time
    repeat(limit) { i ->
        check(account.returnOne()) { "$name: return ${i + 1} should work" }
    }
    check(account.activeLoans == 0)
    check(!account.returnOne()) { "$name: extra return should fail" }
    check(account.activeLoans == 0)

    println("$name (limit $limit): boundary checks ok")
}

fun main() {
    // core: one policy
    val account = Account(StandardPolicy())
    check(account.borrow())
    check(account.activeLoans == 1)
    check(account.returnOne())
    check(!account.returnOne())
    println("standard account borrow/return ok")

    // both policies go through the same code.
    // which limit() runs depends on the real object (interface dispatch)
    val policies: List<LoanPolicy> = listOf(StandardPolicy(), ResearchPolicy())
    for (policy in policies) {
        checkBoundaries(Account(policy))
    }

    // negative limit is rejected in init
    val badPolicy = object : LoanPolicy { override fun limit() = -1 }
    val result = runCatching { Account(badPolicy) }
    check(result.isFailure)
    println("negative limit rejected: ${result.exceptionOrNull()?.message}")

    // val only fixes the reference, the object can still change
    val shelf = mutableListOf("Algorithms")
    shelf.add("Poems")
    // shelf = mutableListOf("Databases")  <- this would not compile
    check(shelf == listOf("Algorithms", "Poems"))

    val sameAccount = account
    sameAccount.borrow()
    check(account.activeLoans == 1)
    println("val test: shelf=$shelf, activeLoans=${account.activeLoans}")

    // pure version
    val s0 = LoanState(limit = 3)
    val t1 = borrow(s0)
    val t2 = borrow(t1.state)
    check(t1.accepted && t2.accepted && t2.state.activeLoans == 2)
    check(s0.activeLoans == 0)
    val full = (1..3).fold(s0) { s, _ -> borrow(s).state }
    check(!borrow(full).accepted && borrow(full).state == full)
    check(!returnOne(s0).accepted)
    println("pure version: s0=$s0, after two borrows=${t2.state}")

    println("all checks passed")
}
