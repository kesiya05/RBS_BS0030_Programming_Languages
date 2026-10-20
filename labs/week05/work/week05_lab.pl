% BS0030 Week 5 Lab - Logic Programming I
% Kesiya's work file for SWI-Prolog.
%
% Load from labs/week05 with:
%   ?- [work/week05_lab].
% Reload after edits:
%   ?- make.

% ---------------------------------------------------------------------------
% Part 0 - facts
% ---------------------------------------------------------------------------
% Atoms: alice, bob, david, clara, daniel, eva, anna, ben, cara, dan, bs0030,
%        bs0013, algorithms, riga, jelgava, siauliai, kaunas, sigulda.
% Variables (capitalised) in the rules: Child, Parent, Grandparent, Grandchild,
%        Student, A, B, C, X, Y, Z, Course, Middle.
% Arity: parent/2, student/1, enrolled/2, passed/2, edge/2.
%
% Queries run:
% ?- parent(alice, bob).    true.
% ?- parent(clara, alice).  false.
% ?- parent(alice, X).      X = bob ; X = david.
% ?- parent(X, clara).      X = bob.
% ?- parent(X, Y).          alice-bob ; alice-david ; bob-clara ;
%                           clara-daniel ; david-eva.
% One fact table supports all these modes because a query is not a function
% call with fixed inputs: Prolog unifies the query with each fact, and any
% argument may be a constant (checked) or a variable (filled in).

parent(alice, bob).
parent(alice, david).
parent(bob, clara).
parent(clara, daniel).
parent(david, eva).

student(anna).
student(ben).
student(cara).
student(dan).

enrolled(anna, bs0030).
enrolled(ben, bs0030).
enrolled(cara, bs0013).
enrolled(dan, bs0030).
enrolled(anna, bs0013).

passed(ben, bs0030).
passed(cara, bs0030).
passed(dan, algorithms).

% A small acyclic route graph for Part 8.
edge(riga, jelgava).
edge(jelgava, siauliai).
edge(siauliai, kaunas).
edge(riga, sigulda).

% ---------------------------------------------------------------------------
% Part 1 - inverse relation
% ---------------------------------------------------------------------------
% Derived from parent/2 by swapping the arguments; no duplicate facts.

child(Child, Parent) :-
    parent(Parent, Child).

% ?- child(bob, alice).   true.
% ?- child(X, alice).     X = bob ; X = david.
% ?- child(clara, X).     X = bob.

% ---------------------------------------------------------------------------
% Part 2 - conjunction / resolution
% ---------------------------------------------------------------------------
% Middle is the shared variable that connects the two parent goals.

grandparent(Grandparent, Grandchild) :-
    parent(Grandparent, Middle),
    parent(Middle, Grandchild).

% ?- grandparent(alice, clara).   true.
% ?- grandparent(alice, X).       X = clara ; X = eva.
% ?- grandparent(X, daniel).      X = bob.
%
% TRACE:
% grandparent(alice, clara)
% -> unify with head: Grandparent = alice, Grandchild = clara
% -> goals: parent(alice, Middle), parent(Middle, clara)
% -> parent(alice, Middle) matches parent(alice, bob): Middle = bob
% -> remaining goal: parent(bob, clara)
% -> parent(bob, clara) matches a fact
% -> success
% (If Middle = bob had failed, Prolog would undo it and try Middle = david.)

% ---------------------------------------------------------------------------
% Part 4 - relations in several modes
% ---------------------------------------------------------------------------

takes_programming_languages(Student) :-
    enrolled(Student, bs0030).

% ?- takes_programming_languages(anna).  true.
% ?- takes_programming_languages(X).     X = anna ; X = ben ; X = dan.

classmate(A, B) :-
    enrolled(A, Course),
    enrolled(B, Course),
    dif(A, B).

% ?- classmate(anna, ben).   true.
% ?- classmate(anna, X).     X = ben ; X = dan ; X = cara.
% ?- classmate(X, Y).        anna-ben ; anna-dan ; ben-anna ; ben-dan ;
%                            cara-anna ; dan-anna ; dan-ben ; anna-cara.
% Symmetry: the rule only says "A and B are enrolled in the same Course and
% are different". Swapping A and B gives the same two enrolled/2 goals, so if
% classmate(anna, ben) is provable, classmate(ben, anna) is provable too.
% dif/2 removes pairs like anna-anna.

% ---------------------------------------------------------------------------
% Part 5 - recursion
% ---------------------------------------------------------------------------

% Base case: one parent edge.
ancestor(X, Y) :-
    parent(X, Y).

% Recursive case: one parent edge to Z, then Z is an ancestor of Y.
% Progress: each step consumes one parent fact before recursing.
ancestor(X, Y) :-
    parent(X, Z),
    ancestor(Z, Y).

% ?- ancestor(alice, bob).      true.
% ?- ancestor(alice, daniel).   true.
% ?- ancestor(alice, X).        bob ; david ; clara ; daniel ; eva.
% ?- ancestor(X, daniel).       clara ; alice ; bob.
% ?- ancestor(daniel, alice).   false.
%
% ancestor(alice, daniel):
%   base case parent(alice, daniel) fails.
%   recursive case: parent(alice, Z) -> Z = bob, need ancestor(bob, daniel)
%     base case parent(bob, daniel) fails
%     recursive: parent(bob, Z2) -> Z2 = clara, need ancestor(clara, daniel)
%       base case parent(clara, daniel) succeeds.
%   Intermediate values of the recursive variable: Z = bob, then Z2 = clara.

% ---------------------------------------------------------------------------
% Part 6 - backtracking through conjunction
% ---------------------------------------------------------------------------

successful_bs0030_student(Student) :-
    student(Student),
    passed(Student, bs0030).

% ?- successful_bs0030_student(X).   X = ben ; X = cara.
%
% TRACE:
% 1. student(Student) -> first candidate Student = anna (choice point left
%    for ben, cara, dan).
% 2. passed(anna, bs0030) -> no matching fact, fails.
% 3. Backtrack to the student/1 choice point: binding Student = anna is UNDONE.
% 4. Next candidate Student = ben; passed(ben, bs0030) succeeds.
%    First solution: X = ben.
% 5. On ; : undo Student = ben, try Student = cara; passed(cara, bs0030)
%    succeeds. Second solution: X = cara.
% 6. On ; : undo cara, try dan; passed(dan, bs0030) fails (dan passed
%    algorithms). No candidates left -> false.

% ---------------------------------------------------------------------------
% Part 7 - goal/clause ordering experiment
% ---------------------------------------------------------------------------

passed_bs0030_a(Student) :-
    student(Student),
    passed(Student, bs0030).

passed_bs0030_b(Student) :-
    passed(Student, bs0030),
    student(Student).

% Deliberately poor recursion. Read and explain; do not wait on a hanging query.
bad_ancestor(X, Y) :-
    bad_ancestor(X, Z),
    parent(Z, Y).
bad_ancestor(X, Y) :-
    parent(X, Y).

% ORDERING NOTES:
% ?- passed_bs0030_a(X).   X = ben ; X = cara.
% ?- passed_bs0030_b(X).   X = ben ; X = cara.
% Both describe the same logical conjunction and give the same answers on this
% finite knowledge base. Version a GENERATES every student (anna, ben, cara,
% dan) and tests each with passed/2, so it fails twice (anna, dan). Version b
% generates only students who passed bs0030 (ben, cara) and then checks
% student/1, so it tries fewer candidates. Same meaning, different search work.
%
% bad_ancestor/2: the first clause calls bad_ancestor(X, Z) immediately, with
% no parent fact consumed first. Prolog always runs the leftmost goal first
% and tries clauses top to bottom, so that call selects the same first clause
% again, which calls bad_ancestor again, and so on. It never reaches the base
% clause (left recursion), so it recurses until the stack overflows.

% ---------------------------------------------------------------------------
% Part 8 - route reachability
% ---------------------------------------------------------------------------

% Direct edge.
reachable(X, Y) :-
    edge(X, Y).

% Edge to an intermediate Z, then continue recursively.
reachable(X, Y) :-
    edge(X, Z),
    reachable(Z, Y).

% ?- reachable(riga, kaunas).       true.
% ?- reachable(riga, D).            jelgava ; sigulda ; siauliai ; kaunas.
% ?- reachable(jelgava, D).         siauliai ; kaunas.
% ?- reachable(Start, kaunas).      siauliai ; riga ; jelgava.
%
% CYCLE NOTE:
% 1. On the acyclic graph every recursive step follows an edge forward, and
%    there are finitely many edges with no way back, so every path is finite
%    and the search ends.
% 2. With edge(kaunas, riga), a query such as reachable(riga, tallinn) (no such
%    destination) follows riga -> jelgava -> siauliai -> kaunas -> riga ->
%    jelgava -> ... forever, revisiting riga and the whole loop.
% 3. Unification and backtracking only remember the bindings of the current
%    proof attempt. Each recursive call is a fresh goal reachable(riga, Y);
%    nothing records that riga was already on the path, so it is explored again.
% 4. A safer predicate needs to carry the path so far (a visited list) and
%    refuse to step to a node already in it.
% 5. Declarative modeling: the two clauses correctly say what reachability
%    means (direct edge, or edge then reachable). Operational search: Prolog's
%    depth-first, left-to-right exploration of proofs, which on a cyclic graph
%    can follow an infinite branch before trying the others.

% ---------------------------------------------------------------------------
% Part 3 - unification notes
% ---------------------------------------------------------------------------
% ?- X = alice.                               X = alice.
% ?- parent(X, bob) = parent(alice, Y).       X = alice, Y = bob.
% ?- pair(X, X) = pair(alice, alice).         X = alice.
% ?- pair(X, X) = pair(alice, bob).           false.
% ?- point(X, 2) = point(1, Y).               X = 1, Y = 2.
% ?- point(X, X) = point(1, 2).               false.

% 1. Why pair(X,X) = pair(alice,bob) fails:
% The first argument binds X = alice. The second argument then needs
% X = bob, but X is already alice and alice does not unify with bob, so the
% whole unification fails. Both occurrences of X must be the same value.
%
% 2. Why = is unification rather than assignment:
% It is symmetric and works on both sides: parent(X, bob) = parent(alice, Y)
% binds variables on the left AND right. It never overwrites a bound variable;
% it either finds a substitution that makes both terms identical or fails.
%
% 3. Structural requirement for compound-term unification:
% The functor name and the arity must be the same (e.g. pair/2 with pair/2);
% only then are the arguments unified pairwise.

% ---------------------------------------------------------------------------
% Part 9 - final explanation
% ---------------------------------------------------------------------------

% 1. Fact vs rule vs query:
% A fact states something unconditionally true (parent(alice, bob).). A rule
% states that the head is true if its body goals are true (grandparent/2). A
% query, typed at ?-, asks Prolog to prove a goal and find variable bindings.
%
% 2. What unification does:
% It finds a substitution for variables that makes two terms identical,
% e.g. parent(X, bob) = parent(alice, Y) gives X = alice, Y = bob, or fails
% if no such substitution exists.
%
% 3. Choice point:
% A place where more than one clause or fact could still match a goal, e.g.
% student(Student) after choosing anna still has ben, cara and dan. Prolog
% remembers it so it can come back on failure or when ; is pressed.
%
% 4. What backtracking restores:
% It returns to the most recent choice point and undoes every variable binding
% made after it (e.g. Student = anna is removed), then tries the next
% alternative.
%
% 5. ancestor/2 versus a one-directional Python function:
% A Python function has fixed inputs and one output. ancestor/2 is a relation,
% so the same code answers ancestor(alice, daniel), ancestor(alice, X) and
% ancestor(X, daniel), returning every solution by backtracking.
%
% 6. Declarative meaning versus operational behavior:
% Clauses can be logically correct but Prolog searches them depth-first, left
% to right, top to bottom. bad_ancestor/2 is logically the same as ancestor/2
% but loops forever, and reachable/2 is correct yet loops on a cyclic graph,
% because the search order follows an infinite branch.
