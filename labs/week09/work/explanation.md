# Week 9 notes

Files: Main.kt (the code) and output.txt (what it printed when I ran it).

To run it from the repo root:

    kotlinc labs/week09/work/Main.kt -include-runtime -d /tmp/week09.jar
    java -jar /tmp/week09.jar

The jar is in /tmp so it doesn't get committed. All the tests use check(), so if something is wrong the program crashes instead of printing "all checks passed".

## Invariant

The rule is 0 <= activeLoans <= limit.

activeLoans has a private setter, so outside the class you can read it but not change it. The only way to change it is borrow() and returnOne(), and both check the rule first. borrow() only works if activeLoans is below the limit, and returnOne() only works if activeLoans is above 0. If the operation isn't allowed it returns false and the count stays the same.

The limit is checked in init with require, so an account with a negative limit is never created. I also store the limit in a val when the account is made, so it stays the same for the whole life of the account.

## Boundary tests

I wrote one function, checkBoundaries, and ran it for both policies. It tests:

- returning when there are no loans (rejected)
- borrowing exactly up to the limit (3 for standard, 5 for research, all accepted)
- borrowing one more (rejected, count doesn't change)
- returning everything and then one more time (last one rejected)

Both policies passed. A policy with limit -1 throws "loan limit must be nonnegative, was -1".

## Interface dispatch and composition

Account doesn't extend a policy, it has one as a field. That's composition. The account does the counting and the policy only tells it the limit.

Account and checkBoundaries only know about the LoanPolicy interface. When policy.limit() is called, the actual object decides which version runs, so StandardPolicy gives 3 and ResearchPolicy gives 5. Because of that I didn't have to change Account or the tests when I added ResearchPolicy.

## val vs mutable objects

    val shelf = mutableListOf("Algorithms")
    shelf.add("Poems")        // works
    // shelf = mutableListOf("Databases")   // doesn't compile

val means shelf always points to the same list, but the list itself can still be changed. Same thing with `val sameAccount = account`: calling sameAccount.borrow() changes the same account object, so account.activeLoans goes up too.

## Extension (pure version)

I also made a version without mutation. LoanState is a data class with the same checks in init. borrow(state) and returnOne(state) don't change the state they get, they return a Transition with accepted and a new state made with copy(). If it's rejected they just return the old state.

The difference from the OOP version is that the old states are still there afterwards (s0 still has 0 loans), and testing is just calling a function and comparing the result. In the OOP version the state is inside the object and gets overwritten. I kept both in Main.kt because the OOP one is the base for Week 10.
