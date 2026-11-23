# Week 10 / A5 notes

Files: Main.kt (code), output.txt (my program's output), contract_output.txt (output of the course contract checks). My week 9 version is still in labs/week09/work/Main.kt.

Commands, from the repo root:

    kotlinc labs/week10/work/Main.kt -include-runtime -d /tmp/week10.jar
    java -jar /tmp/week10.jar

    kotlinc labs/week10/work/Main.kt labs/week10/tests/ContractChecks.kt -include-runtime -d /tmp/week10-tests.jar
    kotlin -classpath /tmp/week10-tests.jar ContractChecksKt

My program ends with "all checks passed" and the contract checks end with "Week 10 baseline contract checks passed."

## What changed from week 9

In week 9 borrow() returned a Boolean. true or false doesn't say why something failed or what the new count is. Now borrow() returns BorrowResult, which is either Granted(activeLoans) with the count after the borrow, or Rejected(reason). returnOne() still returns a Boolean because there's only one way it can fail.

The rest is the same idea: private setter on activeLoans, require in init for a negative limit, limit stored once, StandardPolicy (3) and ResearchPolicy (5) passed in through LoanPolicy.

I used three different ways to handle problems:
- a member that doesn't exist is normal, so findMember returns Member? and gives null
- borrowing when full is an expected outcome, so it's Rejected
- a negative limit is a programming mistake, so the constructor throws with require

## Checks I added

- borrow up to 3 on standard, each Granted with the new count (1, 2, 3)
- 4th borrow is Rejected with a reason and the count stays 3
- return all 3, then an empty return gives false and the count stays 0
- research gives 5 Granted then Rejected
- limit 0 is always Rejected and the count never moves
- limit -1 throws IllegalArgumentException
- findMember(1) is Ada, findMember(99) is null
- firstOrNull with Int, String, Member and an empty list

## == vs ===

Member is a data class, so == compares the values. Member(1, "Ada") == Member(1, "Ada") is true. === checks if both names point to the same object, and two separately created members are different objects, so === is false.

## Generic helper

In fun <T> firstOrNull(xs: List<T>): T?, the same T is used for the list and the result. So a List<Int> gives back Int? and a List<String> gives back String?. The compiler knows the result type without any casting.

## Comparison with a procedural version

A procedural version would keep the count as a plain variable, for example `var activeLoans = 0` at the top of the file, with functions like `fun borrow(limit: Int): Boolean` that change it.

Where state lives: in my version the count is inside each Account object, so two accounts have separate counts. In the procedural version it's one shared variable (or one passed around), and any code can change it.

Where validation happens: in my version every change goes through borrow() and returnOne(), so the check is always done. In the procedural version nothing stops other code from writing `activeLoans = 10` directly and skipping the check.

Aliasing: `val sameAccount = account` doesn't copy anything. Both names are the same object, so a borrow through one is seen through the other. That's fine here because there's still only one place that can change the count. A shallow copy can surprise you though. In the extension, Basket.copy() shares the same list, so adding to b2.ids also changed b1.ids.

Compile-time guarantee: because BorrowResult is sealed, the when in describe() must handle both Granted and Rejected. If I removed one branch it wouldn't compile, and I didn't need an else. Also activeLoans = 5 from outside the class doesn't compile because of the private setter.

Runtime/domain obligation: the type Int doesn't stop a policy from returning -1. That's only caught when the program runs, by require in init. Also the compiler can't check that Granted really holds the new count and not the old one. That's on me, and my checks test it.
