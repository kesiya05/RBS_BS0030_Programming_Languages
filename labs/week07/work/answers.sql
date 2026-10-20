-- BS0030 Week 7 / A4 - Relational querying in SQL
-- Run against the core fixture (outputs are labelled with .print):
--   sqlite3 -header -column work/library.db < work/answers.sql
-- Predictions are written before each query; observed output is in
-- work/output_core.txt and work/output_fixture2.txt.

PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------------------------
-- 0. Fixture check: expect 3 members, 4 books, 4 loans.
-- ---------------------------------------------------------------------------
.print ''
.print '=== 0. Fixture check ==='
SELECT (SELECT COUNT(*) FROM member) AS members,
       (SELECT COUNT(*) FROM book)   AS books,
       (SELECT COUNT(*) FROM loan)   AS loans;

-- ---------------------------------------------------------------------------
-- 1a. Selection + projection: computing titles in stable order.
-- Prediction: Algorithms, Databases. ORDER BY title, then id as a tie-breaker,
-- so the order never depends on storage order.
-- ---------------------------------------------------------------------------
.print ''
.print '=== 1a. Computing titles ==='
SELECT id, title
FROM book
WHERE category = 'computing'
ORDER BY title, id;

-- ---------------------------------------------------------------------------
-- 1b. Distinct categories.
-- Prediction: computing, literature (2 rows, duplicates removed).
-- ---------------------------------------------------------------------------
.print ''
.print '=== 1b. Distinct categories ==='
SELECT DISTINCT category
FROM book
ORDER BY category;

-- ---------------------------------------------------------------------------
-- 2. Active loans with member names and book titles.
-- Grain: one row per ACTIVE LOAN.
-- Prediction: 3 rows (loans 1, 2, 4). Ada appears twice because she has two
-- active loans (Algorithms and Poems); each loan is its own fact.
-- ---------------------------------------------------------------------------
.print ''
.print '=== 2. Active loans with names and titles ==='
SELECT l.id AS loan_id, m.name AS member, b.title AS book
FROM loan AS l
JOIN member AS m ON m.id = l.member_id
JOIN book   AS b ON b.id = l.book_id
WHERE l.returned = 0
ORDER BY l.id;

-- ---------------------------------------------------------------------------
-- 3. Every member and their active-loan count, including zero.
-- Grain: one row per MEMBER.
-- LEFT JOIN keeps members with no matching active loan; the active condition
-- is in ON, not WHERE. COUNT(l.id) counts only non-NULL loan ids, so a member
-- with no match gets 0 (COUNT(*) would wrongly give 1).
-- Prediction: Ada 2, Ben 1, Cara 0.
-- ---------------------------------------------------------------------------
.print ''
.print '=== 3. Every member and active-loan count (LEFT JOIN, filter in ON) ==='
SELECT m.id, m.name, COUNT(l.id) AS active_loans
FROM member AS m
LEFT JOIN loan AS l
  ON l.member_id = m.id
 AND l.returned = 0
GROUP BY m.id, m.name
ORDER BY m.id;

-- 3-bug. The WHERE-versus-ON trap (kept to show the bug).
-- Putting the filter in WHERE runs it AFTER the outer join. Cara's padded row
-- has l.returned = NULL, NULL = 0 is not true, so Cara is filtered away and
-- the outer join silently becomes an inner join.
-- Prediction: Ada 2, Ben 1 - Cara missing.
.print ''
.print '=== 3-bug. WHERE-versus-ON trap (filter in WHERE) ==='
SELECT m.id, m.name, COUNT(l.id) AS active_loans
FROM member AS m
LEFT JOIN loan AS l ON l.member_id = m.id
WHERE l.returned = 0
GROUP BY m.id, m.name
ORDER BY m.id;

-- ---------------------------------------------------------------------------
-- 4. Members with no active loan, using NOT EXISTS.
-- For each member, ask: does an active loan for this member exist? Keep the
-- member only if it does not. Returned loans do not count.
-- Prediction: Cara.
-- ---------------------------------------------------------------------------
.print ''
.print '=== 4. Members with no active loan (NOT EXISTS) ==='
SELECT m.id, m.name
FROM member AS m
WHERE NOT EXISTS (
  SELECT 1
  FROM loan AS l
  WHERE l.member_id = m.id
    AND l.returned = 0
)
ORDER BY m.id;

-- ---------------------------------------------------------------------------
-- 5. Books with at least two active loans (grouped threshold report).
-- WHERE filters loan rows (active only), GROUP BY makes one group per book,
-- HAVING filters groups.
-- Core prediction: Algorithms (Ada and Ben) with 2.
-- Fixture 2 prediction: Algorithms 2 and Poems 2 (Ada and Cara).
-- ---------------------------------------------------------------------------
.print ''
.print '=== 5. Books with at least two active loans ==='
SELECT b.id, b.title, COUNT(*) AS active_loans
FROM loan AS l
JOIN book AS b ON b.id = l.book_id
WHERE l.returned = 0
GROUP BY b.id, b.title
HAVING COUNT(*) >= 2
ORDER BY b.title, b.id;

-- ---------------------------------------------------------------------------
-- 6. Zero-match test for books: every book and its active-loan count.
-- Prediction (core): Algorithms 2, Poems 1, Databases 0 (only a returned
-- loan), Stories 0 (never borrowed).
-- ---------------------------------------------------------------------------
.print ''
.print '=== 6. Every book and active-loan count ==='
SELECT b.id, b.title, COUNT(l.id) AS active_loans
FROM book AS b
LEFT JOIN loan AS l
  ON l.book_id = b.id
 AND l.returned = 0
GROUP BY b.id, b.title
ORDER BY b.id;

-- ---------------------------------------------------------------------------
-- 7. Duplicate semantics: names of members with an active loan.
-- Without DISTINCT the result has one row per active loan, so Ada appears
-- twice; DISTINCT changes the question to "which members".
-- ---------------------------------------------------------------------------
.print ''
.print '=== 7. Member names per active loan, without and with DISTINCT ==='
SELECT m.name
FROM loan AS l JOIN member AS m ON m.id = l.member_id
WHERE l.returned = 0
ORDER BY m.name;

SELECT DISTINCT m.name
FROM loan AS l JOIN member AS m ON m.id = l.member_id
WHERE l.returned = 0
ORDER BY m.name;

-- ---------------------------------------------------------------------------
-- Extension: EXPLAIN QUERY PLAN before and after an index on loan(member_id).
-- The plan is the optimizer's choice and may differ with data size; the
-- index makes an index lookup POSSIBLE, it does not guarantee it is used.
-- ---------------------------------------------------------------------------
.print ''
.print '=== Extension: EXPLAIN QUERY PLAN before / after index ==='
EXPLAIN QUERY PLAN
SELECT m.id, m.name
FROM member AS m
WHERE NOT EXISTS (SELECT 1 FROM loan AS l
                  WHERE l.member_id = m.id AND l.returned = 0);

CREATE INDEX IF NOT EXISTS idx_loan_member_returned ON loan(member_id, returned);

EXPLAIN QUERY PLAN
SELECT m.id, m.name
FROM member AS m
WHERE NOT EXISTS (SELECT 1 FROM loan AS l
                  WHERE l.member_id = m.id AND l.returned = 0);

DROP INDEX idx_loan_member_returned;
