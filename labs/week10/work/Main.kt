// Week 10 lab / A5 - loan account with typed results
// built from my week 9 version (labs/week09/work/Main.kt)
//
// run from repo root:
// kotlinc labs/week10/work/Main.kt -include-runtime -d /tmp/week10.jar
// java -jar /tmp/week10.jar
//
// contract checks:
// kotlinc labs/week10/work/Main.kt labs/week10/tests/ContractChecks.kt -include-runtime -d /tmp/week10-tests.jar
// kotlin -classpath /tmp/week10-tests.jar ContractChecksKt

data class Member(val id: Int, val name: String)

interface LoanPolicy {
    fun limit(): Int
}

class StandardPolicy : LoanPolicy {
    override fun limit(): Int = 3
}

class ResearchPolicy : LoanPolicy {
    override fun limit(): Int = 5
}

// only two possible results, so when() can check both without else
sealed interface BorrowResult
data class Granted(val activeLoans: Int) : BorrowResult   // count after the borrow
data class Rejected(val reason: String) : BorrowResult

// invariant: 0 <= activeLoans <= limit
class Account(policy: LoanPolicy) {
    // read once, so the limit stays the same for this account
    private val limit: Int = policy.limit()

    var activeLoans: Int = 0
        private set

    init {
        require(limit >= 0) { "loan limit must be nonnegative, was $limit" }
    }

    fun borrow(): BorrowResult {
        if (activeLoans >= limit) {
            // nothing changes when rejected (also covers limit 0)
            return Rejected("limit of $limit active loans reached")
        }
        activeLoans++
        return Granted(activeLoans)
    }

    fun returnOne(): Boolean {
        if (activeLoans == 0) return false
        activeLoans--
        return true
    }
}

val members = listOf(Member(1, "Ada"), Member(2, "Ben"))

// a missing member is normal, so return null instead of throwing
fun findMember(id: Int): Member? = members.find { it.id == id }

// T links the list type to the result type: List<Int> gives Int?, List<String> gives String?
fun <T> firstOrNull(xs: List<T>): T? =
    if (xs.isEmpty()) null else xs[0]

fun describe(result: BorrowResult): String =
    when (result) {
        is Granted -> "Active loans: ${result.activeLoans}"
        is Rejected -> "Rejected: ${result.reason}"
    }

fun main() {
    // standard policy: borrow up to the exact limit
    val account = Account(StandardPolicy())
    for (expected in 1..3) {
        val result = account.borrow()
        println(describe(result))
        check(result == Granted(expected))
        check(account.activeLoans == expected)
    }

    // one extra borrow is rejected and the count stays 3
    val extra = account.borrow()
    println(describe(extra))
    check(extra is Rejected && extra.reason.isNotBlank())
    check(account.activeLoans == 3)

    // return all, then one more return fails
    repeat(3) { check(account.returnOne()) }
    check(account.activeLoans == 0)
    check(!account.returnOne())
    check(account.activeLoans == 0)
    println("standard: exact limit, extra borrow, return all, empty return ok")

    // research policy through the same interface
    val research = Account(ResearchPolicy())
    repeat(5) { check(research.borrow() is Granted) }
    check(research.borrow() is Rejected)
    check(research.activeLoans == 5)
    println("research: 5 granted, 6th rejected ok")

    // zero limit: every borrow rejected, count never moves
    val zero = Account(object : LoanPolicy { override fun limit() = 0 })
    check(zero.borrow() is Rejected)
    check(zero.activeLoans == 0)
    println("zero limit: ${describe(zero.borrow())}")

    // negative limit fails in init with IllegalArgumentException
    val bad = runCatching { Account(object : LoanPolicy { override fun limit() = -1 }) }
    check(bad.exceptionOrNull() is IllegalArgumentException)
    println("negative limit: ${bad.exceptionOrNull()?.message}")

    // nullable lookup
    check(findMember(1)?.name == "Ada")
    check(findMember(99) == null)
    println("findMember(1) = ${findMember(1)}, findMember(99) = ${findMember(99)}")
    val name = findMember(99)?.name ?: "no such member"
    println("safe call with default: $name")

    // data class == compares values, === compares if it's the same object
    val a1 = Member(1, "Ada")
    val a2 = Member(1, "Ada")
    check(a1 == a2)
    check(a1 !== a2)
    check(a1 == findMember(1))
    println("a1 == a2: ${a1 == a2}, a1 === a2: ${a1 === a2}")

    // generic helper with two types and an empty list
    val firstId: Int? = firstOrNull(listOf(7, 8))
    val firstName: String? = firstOrNull(listOf("Ada", "Ben"))
    val firstMember: Member? = firstOrNull(members)
    val nothing: Int? = firstOrNull(emptyList<Int>())
    check(firstId == 7 && firstName == "Ada" && firstMember == Member(1, "Ada") && nothing == null)
    println("firstOrNull: $firstId, $firstName, $firstMember, $nothing")

    // extension: copy() on a data class is shallow, the list inside is shared
    data class Basket(val ids: MutableList<Int>)
    val b1 = Basket(mutableListOf(1))
    val b2 = b1.copy()
    b2.ids.add(2)
    check(b1.ids == listOf(1, 2))   // b1 changed too
    check(b1.ids === b2.ids)
    println("shallow copy: b1=${b1.ids}, b2=${b2.ids}")

    println("all checks passed")
}
