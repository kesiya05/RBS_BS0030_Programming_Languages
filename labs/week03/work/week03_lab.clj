;; BS0030 Week 3 Lab
;; Functional Programming I — Kesiya's work file
;;
;; Reload from a REPL with:
;;   (load-file "work/week03_lab.clj")

;; -----------------------------------------------------------------------------
;; Shared data
;; -----------------------------------------------------------------------------

(def numbers [1 2 3 4 5 6])

(def students
  [{:name "Ada"    :score 91}
   {:name "Grace"  :score 78}
   {:name "Alan"   :score 88}
   {:name "Edsger" :score 69}])

;; -----------------------------------------------------------------------------
;; Part 0 — REPL warm-up (evaluated in the REPL)
;; -----------------------------------------------------------------------------
;; (+ 2 3)                 => 5
;; (* 4 (+ 2 3))           => 20
;; (max 5 9 2)             => 9
;; (str "Riga" " " "BS")   => "Riga BS"
;; (inc 10)                => 11
;;
;; The first element of the list decides the operation. Nesting is explicit in
;; the parentheses, so no precedence table is needed: (* 4 (+ 2 3)) is already
;; the syntax tree — the outer list is the root node, the inner list a subtree.

;; -----------------------------------------------------------------------------
;; Part 1 — bindings
;; -----------------------------------------------------------------------------
;; (let [x 10] (let [x 20] x))  => 20
;; There are two separate bindings named x. The inner one shadows the outer one
;; inside the inner body. The value 10 is never mutated; it is just not visible there.

(defn invoice-total [price quantity]
  (* price quantity))

;; (invoice-total 12 4)   => 48
;; (invoice-total 7.5 3)  => 22.5

;; -----------------------------------------------------------------------------
;; Part 2 — pure functions
;; -----------------------------------------------------------------------------
;; Both functions depend only on their argument and change no outside state,
;; so the same input always gives the same output.

(defn square [x]
  (* x x))

(defn celsius->fahrenheit [c]
  (+ (/ (* c 9.0) 5.0) 32.0))

;; (square 5)                  => 25
;; (celsius->fahrenheit 0)     => 32.0
;; (celsius->fahrenheit 100)   => 212.0

;; -----------------------------------------------------------------------------
;; Part 3 — functions as values
;; -----------------------------------------------------------------------------
;; apply-twice is a higher-order function because it takes a function f as an
;; argument and calls it.

(defn apply-twice [f x]
  (f (f x)))

;; (apply-twice inc 10)                 => 12
;; (apply-twice square 2)               => 16
;; (apply-twice (fn [x] (* 2 x)) 3)     => 12
;; (apply-twice #(* 2 %) 3)             => 12

;; -----------------------------------------------------------------------------
;; Part 4 — immutable collections (evaluated in the REPL)
;; -----------------------------------------------------------------------------
;; (conj numbers 7)          => [1 2 3 4 5 6 7]
;; numbers                   => [1 2 3 4 5 6]   <- unchanged, conj returned a new vector
;; (first numbers)           => 1
;; (rest numbers)            => (2 3 4 5 6)
;; (:name (first students))  => "Ada"
;; (:score (first students)) => 91

;; -----------------------------------------------------------------------------
;; Part 5 — map
;; -----------------------------------------------------------------------------
;; Pattern: one input element -> exactly one transformed output element.

(defn student-names [student-coll]
  (map :name student-coll))

;; (student-names students)  => ("Ada" "Grace" "Alan" "Edsger")
;; (map square numbers)      => (1 4 9 16 25 36)

;; -----------------------------------------------------------------------------
;; Part 6 — filter
;; -----------------------------------------------------------------------------
;; The predicate receives one student map and returns true/false. filter does
;; not change the record; it only decides whether to keep it.

(defn passing-students [student-coll threshold]
  (filter (fn [student] (>= (:score student) threshold)) student-coll))

;; (passing-students students 80)
;; => ({:name "Ada", :score 91} {:name "Alan", :score 88})

;; -----------------------------------------------------------------------------
;; Part 7 — reduce
;; -----------------------------------------------------------------------------
;; The accumulator (acc) carries the running total from step to step instead of
;; a mutable variable.

(defn total-score [student-coll]
  (reduce (fn [acc student] (+ acc (:score student))) 0 student-coll))

(defn average-score [student-coll]
  (/ (double (total-score student-coll)) (count student-coll)))

;; (total-score students)    => 326
;; (average-score students)  => 81.5

;; -----------------------------------------------------------------------------
;; Part 8 — pipeline
;; -----------------------------------------------------------------------------
;; Trace for threshold 80:
;;   all students
;;     ↓ filter (score >= 80)
;;   ({:name "Ada", :score 91} {:name "Alan", :score 88})
;;     ↓ map :name
;;   ("Ada" "Alan")

(defn names-at-or-above [student-coll threshold]
  (->> student-coll
       (filter #(>= (:score %) threshold))
       (map :name)))

;; (names-at-or-above students 80)  => ("Ada" "Alan")

;; -----------------------------------------------------------------------------
;; Part 9 — recursion
;; -----------------------------------------------------------------------------
;; Base case: empty collection -> 0.
;; Recursive case: first element + sum of the rest (a smaller input).

(defn sum-recursive [xs]
  (if (empty? xs)
    0
    (+ (first xs) (sum-recursive (rest xs)))))

;; (sum-recursive [])         => 0
;; (sum-recursive [1 2 3 4])  => 10
;; (sum-recursive numbers)    => 21, same as (reduce + numbers) => 21.
;; (reduce + numbers) states the intent "sum the collection" more directly.

;; -----------------------------------------------------------------------------
;; Part 10 — imperative -> functional rewrite
;; -----------------------------------------------------------------------------
;; Trace for [1 2 3 4 5 6]:
;;   filter even?        -> (2 4 6)
;;   map square          -> (4 16 36)
;;   filter #(> % 10)    -> (16 36)
;;   count               -> 2

(defn count-large-even-squares [values]
  (->> values
       (filter even?)
       (map square)
       (filter #(> % 10))
       count))

;; (count-large-even-squares [1 2 3 4 5 6])  => 2
;; (count-large-even-squares [2 4 6 8])      => 3

;; -----------------------------------------------------------------------------
;; Part 11 — reflection
;; -----------------------------------------------------------------------------

;; 1. In the Python loop from assignment.md, which values are explicitly mutated?
;; ANSWER: The counter `count` is mutated with `count += 1`, and the loop
;; variable `x` and the temporary `square` are reassigned on every iteration.
;;
;; 2. In your Clojure rewrite, what replaces the mutable counter and explicit
;;    traversal loop?
;; ANSWER: The loop is replaced by filter and map, which walk the collection for
;; me, and the counter is replaced by `count` on the final sequence. Each step
;; returns a new value instead of updating a variable.
;;
;; 3. Give one example from this lab where a function is passed as a value.
;; ANSWER: (apply-twice inc 10) passes `inc` itself to apply-twice. Also
;; (filter even? values) and (map square numbers).
;;
;; 4. Give one example of an immutable collection operation that returns a new
;;    value.
;; ANSWER: (conj numbers 7) returns [1 2 3 4 5 6 7], while `numbers` is still
;; [1 2 3 4 5 6]. (assoc student :score 95) works the same way for maps.
;;
;; 5. When is explicit recursion less clear than map, filter, or reduce?
;; ANSWER: When the task is a standard pattern like "transform each element",
;; "keep some elements" or "combine into one value". Then the base case and
;; first/rest plumbing hide the intent, while (reduce + xs) says it directly.
;; Plain recursion is also not stack-safe for very long collections.

;; -----------------------------------------------------------------------------
;; Extensions
;; -----------------------------------------------------------------------------

;; E1. Recursive map.
(defn my-map [f xs]
  (if (empty? xs)
    '()
    (cons (f (first xs)) (my-map f (rest xs)))))
;; (my-map square [1 2 3]) => (1 4 9)

;; E2. Higher-order function that returns a predicate.
(defn make-threshold-predicate [threshold]
  (fn [student] (>= (:score student) threshold)))
;; (filter (make-threshold-predicate 80) students)
;; => ({:name "Ada", :score 91} {:name "Alan", :score 88})

;; E3. sum with loop/recur: tail-recursive, so it does not grow the stack.
;; The recursive version builds up pending additions; this one carries the
;; accumulator forward like reduce.
(defn sum-loop [xs]
  (loop [remaining xs
         acc 0]
    (if (empty? remaining)
      acc
      (recur (rest remaining) (+ acc (first remaining))))))
;; (sum-loop numbers) => 21

;; E4. frequencies of score categories. Conceptually this is a reduce: it folds
;; the whole sequence into one map of counts (it is implemented with reduce).
(defn score-category [score]
  (cond (>= score 85) :high
        (>= score 75) :medium
        :else :low))
;; (frequencies (map (comp score-category :score) students))
;; => {:high 2, :medium 1, :low 1}

;; E5. (map inc [1 2 3]) returns a lazy sequence (2 3 4) (type LazySeq),
;; while [2 3 4] is a vector (PersistentVector). They are equal with =, but the
;; first is a sequence produced on demand, the second a concrete indexed collection.
