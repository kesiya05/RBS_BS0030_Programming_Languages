# Week 7 / A4 - Relational Querying in SQL

## Files

| File | Purpose |
|---|---|
| `setup.sql` | Exact Week 7 core fixture (3 members, 4 books, 4 loans) |
| `fixture2.sql` | Second controlled fixture: adds loan 5 (Cara borrows Poems, active) |
| `answers.sql` | All queries, each with a written prediction |
| `output_core.txt` | Observed output on the core fixture |
| `output_fixture2.txt` | Observed output on the second fixture |

## How to run

From `labs/week07` (Core Codespace, `sqlite3 --version` 3.x):

```bash
# core fixture
rm -f work/library.db
sqlite3 work/library.db < work/setup.sql
sqlite3 -header -column work/library.db < work/answers.sql

# second fixture
rm -f work/library2.db
sqlite3 work/library2.db < work/setup.sql
sqlite3 work/library2.db < work/fixture2.sql
sqlite3 -header -column work/library2.db < work/answers.sql
```

The `.db` files are disposable and are not committed.

## Keys and predicted join size (workshop step 1)

```
member(id PK) <--- loan(member_id FK)
book(id PK)   <--- loan(book_id FK)
```

`loan` is the relationship table: each loan row points to exactly one member and one book. Active loans are rows with `returned = 0`: loans 1, 2 and 4. Joining active loans to `member` and `book` along the foreign keys keeps one row per active loan, so I predicted **3 rows**. Observed: 3 rows.

## Predictions vs observed results (core fixture)

| Query | Prediction | Observed |
|---|---|---|
| 0 Fixture check | 3 / 4 / 4 | 3 / 4 / 4 |
| 1a Computing titles | Algorithms, Databases | Algorithms, Databases |
| 1b Distinct categories | computing, literature | computing, literature |
| 2 Active loans | Ada-Algorithms, Ada-Poems, Ben-Algorithms | same |
| 3 Active count per member | Ada 2, Ben 1, Cara 0 | same |
| 3-bug WHERE version | Cara missing | Ada 2, Ben 1 (Cara missing) |
| 4 NOT EXISTS | Cara | Cara |
| 5 Books with >= 2 active loans | Algorithms 2 | Algorithms 2 |
| 6 Active count per book | Algorithms 2, Poems 1, Databases 0, Stories 0 | same |
| 7 Names without / with DISTINCT | Ada, Ada, Ben / Ada, Ben | same |

## Second fixture

`fixture2.sql` adds `loan(5, member 3 Cara, book 2 Poems, active)`. Now another member is borrowing a book that already has an active loan.

| Query | Prediction | Observed |
|---|---|---|
| 3 Active count per member | Ada 2, Ben 1, Cara 1 | same |
| 3-bug WHERE version | same as 3 (nobody has zero now) | same - the bug is hidden when every member has a match |
| 4 NOT EXISTS | no rows | no rows |
| 5 Threshold report | Algorithms 2, Poems 2 | Algorithms 2, Poems 2 |
| 6 Active count per book | Algorithms 2, Poems 2, Databases 0, Stories 0 | same |

The threshold report changes only because the data changed, not the query. The second fixture also shows why the WHERE-versus-ON bug is dangerous: it gives the right answer as long as every member has an active loan, and only fails when a zero-match row exists. That is why the zero-match cases (Cara in the core fixture, Databases and Stories in both) are needed as tests.

**Zero-match books:**
- Stories was never borrowed.
- Databases has only a returned loan (loan 3).

Both get 0, which shows the filter is on *active* loans and not just any loan row.

## Result grain

The grain is what one output row represents.

- **Query 2:** one row per active loan. Ada has two active loans, so she appears twice. This is correct, not a duplicate error: each row is a different fact (loan 1 vs loan 2).
- **Query 3:** one row per member. `GROUP BY m.id` collapses that member's loans into a count.
- **Query 5:** one row per book. `WHERE` filters loan rows before grouping, and `HAVING` filters the groups afterwards.

## Counting zero and the outer-join filter trap

To report every member, the join must be a `LEFT JOIN`. An inner join drops Cara because no loan row matches her. With `LEFT JOIN`, Cara is kept and the loan columns are filled with `NULL`.

- **Filter in `ON`:** "join only the active loans". It decides which loan rows match, but every member is still kept.
- **Filter in `WHERE`:** applied after the join, to the padded rows. For Cara, `l.returned` is `NULL`, and `NULL = 0` is not true, so her row is removed. The outer join silently turns back into an inner join.

`COUNT(l.id)` counts only non-NULL values, so Cara's padded row counts as 0. `COUNT(*)` would count the padded row itself and wrongly report 1.

## Duplicate semantics

SQL tables and query results are bags (multisets), not sets: identical rows are allowed and kept.

- Query 7 without `DISTINCT` lists Ada twice, because the question "a name for each active loan" really has two Ada answers.
- `DISTINCT` turns the question into "which members have an active loan".
- Query 1b uses `DISTINCT` because the question is about categories, not books.

`DISTINCT` should match the intended grain, not be added to hide unexpected duplicates.

## One query in relational vs imperative terms (Query 4, NOT EXISTS)

**Relational / declarative:** the result is the set of members `m` for which there is no loan `l` with `l.member_id = m.id` and `l.returned = 0`. I describe *which* rows I want. The database decides how to find them.

**Imperative equivalent:**

```python
result = []
for m in members:
    found = False
    for l in loans:
        if l.member_id == m.id and l.returned == 0:
            found = True
            break
    if not found:
        result.append(m)
```

The SQL version has no loop variable, no `found` flag and no explicit order of work. The plan in the extension shows that SQLite chooses that strategy itself.

## Extension: EXPLAIN QUERY PLAN

Observed output for the NOT EXISTS query:

```
before index:
|--SCAN m
`--CORRELATED SCALAR SUBQUERY 1
   `--SCAN l

after CREATE INDEX idx_loan_member_returned ON loan(member_id, returned):
|--SCAN m
`--CORRELATED SCALAR SUBQUERY 1
   `--SEARCH l USING COVERING INDEX idx_loan_member_returned (member_id=? AND returned=?)
```

- **Before the index:** for every member, SQLite scans the whole `loan` table.
- **After the index:** it can search directly for that member's active loans, and the index covers both columns used in the subquery.

The result rows are identical either way; only the access path changes. The index makes a search possible but does not force it. The optimizer chooses a plan from table sizes and statistics, and on a different dataset it could still choose a scan. The script drops the index again so the queries can be rerun on the same database.
