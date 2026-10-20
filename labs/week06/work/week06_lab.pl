% BS0030 Week 6 Lab - Logic Programming II
% Kesiya's work file: lists, safe search, negation, and CLP(FD).
% Load with  ?- [work/week06_lab].   Reload after edits with  ?- make.

:- use_module(library(clpfd)).

% =============================================================================
% Part 1 - recursive list relations
% =============================================================================

% member_of(X, List): X is an element of List.
% Base case: X is the head. Recursive case: X is a member of the tail.
member_of(X, [X|_]).
member_of(X, [_|Tail]) :-
    member_of(X, Tail).

% append_to(Xs, Ys, Zs): Zs is Xs followed by Ys.
% Base case: appending to [] gives Ys. Recursive case: the head of Xs is also
% the head of Zs, and the tails are related by append_to.
append_to([], Ys, Ys).
append_to([H|T], Ys, [H|Zs]) :-
    append_to(T, Ys, Zs).

% Queries tested:
% ?- member_of(20, [10,20,30]).     true
% ?- member_of(X, [10,20,30]).      X = 10 ; X = 20 ; X = 30 ; false.
% ?- append_to([1,2], [3,4], Z).    Z = [1,2,3,4].
% ?- append_to(A, B, [1,2,3]).
%    A = [],      B = [1,2,3] ;
%    A = [1],     B = [2,3] ;
%    A = [1,2],   B = [3] ;
%    A = [1,2,3], B = [] ; false.
%
% Reflection: why can append_to(A,B,[1,2,3]) generate several splits?
% append_to describes a RELATION between three lists, not a function from two
% inputs to one output. Any argument can be left unbound, so with only Zs known
% Prolog uses unification and backtracking to find every pair A, B whose
% concatenation is [1,2,3]. The same two clauses append, split, or check lists
% depending on which arguments are instantiated (different modes).

% =============================================================================
% Part 2 - cycle-safe route search
% =============================================================================

edge(riga, kaunas).
edge(kaunas, riga).
edge(kaunas, vilnius).
edge(vilnius, kaunas).
edge(riga, tallinn).
edge(tallinn, helsinki).
edge(helsinki, tallinn).
edge(vilnius, warsaw).

route(From, To, Path) :-
    route_(From, To, [From], ReversePath),
    reverse(ReversePath, Path).

% route_(Current, Goal, Visited, Path)
% Visited holds the current path in reverse (most recent node first).
% Base case: we are at the goal, so the visited list IS the (reversed) path.
route_(To, To, Visited, Visited).
% Recursive case: step along an edge to a node not already on the path.
route_(From, To, Visited, Path) :-
    edge(From, Next),
    \+ member_of(Next, Visited),
    route_(Next, To, [Next|Visited], Path).

% Queries tested:
% ?- route(riga, vilnius, Path).   Path = [riga, kaunas, vilnius] ; false.
% ?- route(riga, warsaw, Path).    Path = [riga, kaunas, vilnius, warsaw] ; false.
% ?- route(riga, helsinki, Path).  Path = [riga, tallinn, helsinki] ; false.
% ?- route(riga, berlin, _).       false.   (terminates)
%
% Reflection: cycles, termination, and the Visited invariant.
% Naive depth-first reachability (reach(X,Y) :- edge(X,Z), reach(Z,Y)) can walk
% riga -> kaunas -> riga -> kaunas ... forever, so a query for an unreachable
% node like berlin never answers false (infinite loop / stack overflow), and
% reachable goals can produce infinitely many repeated paths.
% Invariant: Visited contains exactly the nodes on the current path and they
% are all distinct. Each step adds one new node, and the graph is finite, so
% every path is finite and the whole search terminates.

% =============================================================================
% Part 3 - negation as failure and goal order
% =============================================================================

student(alice).
student(bob).
student(carla).

passed(alice).
passed(carla).

% Generate a concrete student FIRST, then test that they have not passed.
needs_retry(Student) :-
    student(Student),
    \+ passed(Student).

bad_needs_retry(Student) :-
    \+ passed(Student),
    student(Student).

% Queries tested:
% ?- needs_retry(X).       X = bob.
% ?- bad_needs_retry(X).   false.
% ?- bad_needs_retry(bob). true.   (works only when Student is already bound)
%
% Reflection: why does order matter, and why is \+ not classical negation?
% \+ is negation as failure: \+ G succeeds exactly when Prolog fails to prove G
% from the facts it has (closed-world assumption). It is not classical logical
% negation ("G is false"); it binds no variables and only checks provability.
% In bad_needs_retry, X is still unbound, so \+ passed(X) asks "is there NO X
% that passed?". passed(alice) exists, so \+ fails and the query returns false,
% even though bob needs a retry. In needs_retry, student/1 first binds Student
% to alice, bob, carla in turn, so \+ passed(Student) tests one concrete,
% sufficiently instantiated candidate at a time and correctly yields bob.

% =============================================================================
% Part 4 - =, is, and #=
% =============================================================================
% ?- X = 2 + 3.                   X = 2+3.   (the term +(2,3), nothing computed)
% ?- X is 2 + 3.                  X = 5.
% ?- X in 1..10, X + 3 #= 8.      X = 5.
%
% = is unification: it makes two terms structurally identical; 2+3 stays a
%   compound term and no arithmetic happens.
% is evaluates the right-hand side arithmetically and unifies the result with
%   the left; the right side must already be fully bound (X is Y+3 with Y
%   unbound gives an instantiation error).
% #= is a CLP(FD) constraint: a true arithmetic relation over integers that
%   works in any direction. With X in 1..10 and X+3 #= 8, the solver deduces
%   X = 5 even though X was unknown, which `is` cannot do.

% =============================================================================
% Part 5 - propagation and labeling
% =============================================================================

pair_sum(X, Y) :-
    [X, Y] ins 1..10,
    X + Y #= 10,
    X #< Y.

% ?- pair_sum(X, Y).
%    X in 1..8, X+Y #= 10, X #=< Y + -1, Y in 2..9.
% ?- pair_sum(X, Y), labeling([], [X,Y]).
%    X = 1, Y = 9 ; X = 2, Y = 8 ; X = 3, Y = 7 ; X = 4, Y = 6 ; false.
%
% Reflection: compare before and after labeling.
% Without labeling there are no concrete values, but propagation has already
% narrowed the domains from 1..10 to X in 1..8 and Y in 2..9 (X < Y removes
% X = 10 / Y = 1, and X + Y = 10 then removes X = 9 / Y = 1 and Y = 10), and
% the remaining constraints are shown as residual goals. Propagation is not
% complete reasoning: it did not discover that X must be 1..4. labeling/2 then
% starts enumeration inside the reduced domains and finds the four concrete
% solutions on backtracking.

% =============================================================================
% Part 6 - scheduling with CLP(FD)
% =============================================================================

% Tasks a, b, c in distinct slots 1..3; a before c; b not in slot 1.
% This predicate only POSTS constraints; enumeration is done by the caller.
schedule(A, B, C) :-
    [A, B, C] ins 1..3,
    all_distinct([A, B, C]),
    A #< C,
    B #\= 1.

% ?- schedule(A, B, C), labeling([], [A,B,C]).
%    A = 1, B = 2, C = 3 ;
%    A = 1, B = 3, C = 2 ;
%    false.
% Check: both use slots 1,2,3 once; A < C holds (1<3, 1<2); B is 2 or 3, never 1.
% Propagation alone already fixes A = 1: B can't be 1, and C > A so C can't be
% 1, leaving slot 1 only for A.

% =============================================================================
% Extension - N-Queens
% =============================================================================

% Board as a list: position = column, value = row of that column's queen.
% Variables: Qs (one per column). Domains: 1..N. Constraints: all rows
% different, and no two queens on a diagonal. Enumeration starts at labeling.
n_queens(N, Qs) :-
    length(Qs, N),
    Qs ins 1..N,
    all_distinct(Qs),
    safe_diagonals(Qs),
    labeling([ff], Qs).

safe_diagonals([]).
safe_diagonals([Q|Qs]) :-
    no_attack(Q, Qs, 1),
    safe_diagonals(Qs).

% D = column distance between Q and the current queen Q1.
% Same diagonal means row difference equals column distance.
no_attack(_, [], _).
no_attack(Q, [Q1|Qs], D) :-
    abs(Q - Q1) #\= D,
    D1 is D + 1,
    no_attack(Q, Qs, D1).

% ?- n_queens(4, Qs).   Qs = [2,4,1,3] ; Qs = [3,1,4,2] ; false.
% ?- n_queens(8, Qs).   Qs = [1,5,8,6,3,7,2,4] ; ...
