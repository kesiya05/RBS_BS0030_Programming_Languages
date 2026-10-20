;; BS0030 Week 4 — A2 Functional Data Processing
;; Kesiya's work file
;;
;; Reload from a REPL with:
;;   (load-file "work/week04_a2.clj")

(require '[clojure.string :as str])

;; -----------------------------------------------------------------------------
;; Shared immutable dataset
;; -----------------------------------------------------------------------------

(def students
  [{:id 1 :name " Ada Lovelace "    :score 91 :active true  :group :a
    :stats {:attempts 2 :late 0}}
   {:id 2 :name "Grace Hopper"      :score 58 :active true  :group :b
    :stats {:attempts 3 :late 1}}
   {:id 3 :name "Alan Turing"       :score 88 :active false :group :a
    :stats {:attempts 1 :late 0}}
   {:id 4 :name " Edsger Dijkstra " :score 76 :active true  :group :b
    :stats {:attempts 2 :late 0}}
   {:id 5 :name "Barbara Liskov"    :score 84 :active true  :group :a
    :stats {:attempts 1 :late 0}}
   {:id 6 :name "John McCarthy"     :score 67 :active true  :group :c
    :stats {:attempts 2 :late 1}}])

;; -----------------------------------------------------------------------------
;; Part 0 — retrieval and prediction
;; -----------------------------------------------------------------------------
;; Binding vs mutation: a binding gives a name to a value; mutation changes the
;; value in place. Clojure locals are bindings - nothing is overwritten.
;; map: one output element per input element. filter: keeps elements for which
;; a predicate is true. reduce: folds a collection into one value with an
;; accumulator. A pure function depends only on its arguments and has no effects.
;; conj returns a new vector that shares structure with the old one, so the old
;; vector is untouched and still usable.
;;
;; (def original (first students))
;; (def changed  (assoc original :score 95))
;; (:score original) => 91   (prediction: 91 - assoc does not change original)
;; (:score changed)  => 95

;; -----------------------------------------------------------------------------
;; Part 1 — persistent immutable updates
;; -----------------------------------------------------------------------------
;; Both return NEW maps. The input map is never changed, so every version
;; (ada, ada2, ada3) stays accessible at the same time. This is "persistence"
;; in the data-structure sense: old versions survive updates (sharing unchanged
;; parts), not saving to disk.

(defn update-score [student new-score]
  (assoc student :score new-score))

(defn record-attempt [student]
  (update-in student [:stats :attempts] inc))

;; (def ada  (first students))
;; (def ada2 (update-score ada 95))
;; (def ada3 (record-attempt ada))
;; (:score ada)                       => 91
;; (:score ada2)                      => 95
;; (get-in ada  [:stats :attempts])   => 2
;; (get-in ada3 [:stats :attempts])   => 3
;; (:stats ada3) => {:attempts 3, :late 0} while (:stats ada) is still {:attempts 2, :late 0}

;; -----------------------------------------------------------------------------
;; Part 2 — closure
;; -----------------------------------------------------------------------------
;; The returned fn "closes over" the parameter `minimum`. After
;; minimum-score-checker returns, the inner function still carries that binding
;; with it, so it reads minimum from its captured lexical environment.

(defn minimum-score-checker [minimum]
  (fn [student]
    (>= (:score student) minimum)))

;; (def score-at-least-70? (minimum-score-checker 70))
;; (score-at-least-70? (first students))   => true   (Ada, 91)
;; (score-at-least-70? (second students))  => false  (Grace, 58)

;; -----------------------------------------------------------------------------
;; Part 3 — composition
;; -----------------------------------------------------------------------------
;; comp applies functions right to left: str/trim runs FIRST on the input, then
;; str/upper-case runs on the trimmed result.
;; (normalize-name " Ada Lovelace ") = (str/upper-case (str/trim " Ada Lovelace "))

(def normalize-name
  (comp str/upper-case str/trim))

;; (normalize-name " Ada Lovelace ")  => "ADA LOVELACE"
;; (normalize-name "Grace Hopper")    => "GRACE HOPPER"

;; -----------------------------------------------------------------------------
;; Part 4 — main thread-last pipeline
;; -----------------------------------------------------------------------------
;; ->> inserts the result of each step as the LAST argument of the next step.
;; Equivalent nested calls:
;;   (vec (sort (map normalize-name (map :name
;;     (filter (minimum-score-checker minimum) (filter :active student-coll)))))))
;;
;; Intermediate values for minimum 70:
;;   students                      6 records
;;   filter :active                ids 1 2 4 5 6       (Alan Turing removed, inactive)
;;   filter score >= 70            ids 1 4 5           (Grace 58, John 67 removed)
;;   map :name                     (" Ada Lovelace " " Edsger Dijkstra " "Barbara Liskov")
;;   map normalize-name            ("ADA LOVELACE" "EDSGER DIJKSTRA" "BARBARA LISKOV")
;;   sort                          ("ADA LOVELACE" "BARBARA LISKOV" "EDSGER DIJKSTRA")
;;   vec                           ["ADA LOVELACE" "BARBARA LISKOV" "EDSGER DIJKSTRA"]

(defn eligible-names [student-coll minimum]
  (->> student-coll
       (filter :active)
       (filter (minimum-score-checker minimum))
       (map :name)
       (map normalize-name)
       sort
       vec))

;; (eligible-names students 70)
;; => ["ADA LOVELACE" "BARBARA LISKOV" "EDSGER DIJKSTRA"]
;; (eligible-names students 60)
;; => ["ADA LOVELACE" "BARBARA LISKOV" "EDSGER DIJKSTRA" "JOHN MCCARTHY"]

;; -----------------------------------------------------------------------------
;; Part 5 — aggregation and pure report construction
;; -----------------------------------------------------------------------------

(defn average-score [student-coll]
  (if (empty? student-coll)
    0.0
    (/ (double (reduce + (map :score student-coll)))
       (count student-coll))))

;; (average-score students)  => 77.33333333333333   (464 / 6)
;; (average-score [])        => 0.0

;; Pure: same inputs always give the same map; no printing, files, time or randomness.
(defn build-report [student-coll minimum]
  (let [active (filter :active student-coll)]
    {:minimum-score        minimum
     :active-count         (count active)
     :average-active-score (average-score active)
     :eligible-names       (eligible-names student-coll minimum)}))

;; (build-report students 70)
;; => {:minimum-score 70, :active-count 5, :average-active-score 75.2,
;;     :eligible-names ["ADA LOVELACE" "BARBARA LISKOV" "EDSGER DIJKSTRA"]}
;; active scores: 91 58 76 84 67 -> 376 / 5 = 75.2

;; -----------------------------------------------------------------------------
;; Part 6 — laziness experiment
;; -----------------------------------------------------------------------------

(defn traced-scores [student-coll]
  ;; Intentionally effectful INSIDE a lazy map for observation.
  ;; Do not copy this design into build-report.
  (map (fn [student]
         (println "realizing student" (:id student))
         (:score student))
       student-coll))

;; Observed in the REPL:
;; (def observed (traced-scores students))
;;     -> nothing printed. map only described the work and returned a lazy seq.
;; (first observed)
;;     -> printed "realizing student 1" ... "realizing student 6", returned 91.
;;        Asking for ONE element realized all six, because the input is a vector
;;        and Clojure realizes vector-backed sequences in chunks (of up to 32).
;; (doall observed)
;;     -> nothing new printed, returned (91 58 88 76 84 67). The work was already
;;        done by the chunk, so doall only walked the cached values.
;; (first observed)
;;     -> nothing printed, returned 91. Realized elements are cached, not recomputed.

;; -----------------------------------------------------------------------------
;; Part 7 — effectful shell
;; -----------------------------------------------------------------------------
;; The only function that prints. It reads every value from the report map.

(defn print-report! [report]
  (println "Minimum score:"        (:minimum-score report))
  (println "Active students:"      (:active-count report))
  (println "Average active score:" (:average-active-score report))
  (println "Eligible names:"       (str/join ", " (:eligible-names report))))

;; -----------------------------------------------------------------------------
;; Part 8 — integration examples
;; -----------------------------------------------------------------------------
;; (def report70 (build-report students 70))
;; report70
;; (print-report! report70)
;;   Minimum score: 70
;;   Active students: 5
;;   Average active score: 75.2
;;   Eligible names: ADA LOVELACE, BARBARA LISKOV, EDSGER DIJKSTRA
;;
;; (build-report students 60)
;; => {:minimum-score 60, :active-count 5, :average-active-score 75.2,
;;     :eligible-names ["ADA LOVELACE" "BARBARA LISKOV" "EDSGER DIJKSTRA" "JOHN MCCARTHY"]}
;; Only the argument changed; the pipeline was not rewritten.

;; -----------------------------------------------------------------------------
;; Extensions
;; -----------------------------------------------------------------------------

;; E1. Per-group averages with group-by (pure).
(defn group-averages [student-coll]
  (->> student-coll
       (group-by :group)
       (map (fn [[group members]] [group (average-score members)]))
       (into (sorted-map))))
;; (group-averages students) => {:a 87.66666666666667, :b 67.0, :c 67.0}

;; E2. Eligible and ineligible split from one pure transformation.
(defn split-by-eligibility [student-coll minimum]
  (let [eligible? (every-pred :active (minimum-score-checker minimum))]
    {:eligible   (mapv (comp normalize-name :name) (filter eligible? student-coll))
     :ineligible (mapv (comp normalize-name :name) (remove eligible? student-coll))}))
;; (split-by-eligibility students 70)
;; => {:eligible ["ADA LOVELACE" "EDSGER DIJKSTRA" "BARBARA LISKOV"],
;;     :ineligible ["GRACE HOPPER" "ALAN TURING" "JOHN MCCARTHY"]}

;; E3. Second closure: a group predicate factory.
(defn in-group? [group]
  (fn [student] (= (:group student) group)))
;; (map :id (filter (in-group? :a) students)) => (1 3 5)

;; E4. map vs mapv: (mapv f xs) returns a vector and is eager - with
;; traced-scores logic it would print for every student immediately, while map
;; prints nothing until the sequence is realized.

;; -----------------------------------------------------------------------------
;; Part 9 — reflection (1–3 sentences for each)
;; -----------------------------------------------------------------------------

;; 1. Persistent values and mutable aliases:
;;
;; Because update-score and record-attempt return new maps, `ada`, `ada2` and
;; `ada3` can all be used at the same time and none of them changes behind my
;; back. With mutable objects, two names pointing to the same record would both
;; see a change, so I would have to track every alias; here a value never changes
;; once created.
;;
;; 2. What binding does your closure capture, and why is that useful?
;;
;; minimum-score-checker captures the `minimum` parameter. That lets me build a
;; configured predicate such as (minimum-score-checker 70) once and pass it to
;; filter, so eligible-names and build-report work for any threshold (70 or 60)
;; without hard-coding it.
;;
;; 3. What did you observe in the laziness experiment?
;;
;; (def observed (traced-scores students)) printed nothing: creating the lazy
;; sequence only describes the work. (first observed) printed all six
;; "realizing student" lines, not just one, because the vector is realized in a
;; chunk; doall and the second (first observed) printed nothing more, since
;; realized values are cached and not recomputed.
;;
;; 4. Which functions form your functional core, and which function is
;;    the effectful shell?
;;
;; The functional core is update-score, record-attempt, minimum-score-checker,
;; normalize-name, eligible-names, average-score and build-report: they only
;; compute and return values. print-report! is the effectful shell: it takes the
;; finished report map and prints it, and no printing happens anywhere else
;; (traced-scores is only the deliberate laziness experiment).
