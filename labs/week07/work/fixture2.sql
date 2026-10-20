-- Second controlled fixture for A4.
-- Run AFTER setup.sql on a fresh database:
--   sqlite3 work/library2.db < work/setup.sql
--   sqlite3 work/library2.db < work/fixture2.sql
--
-- Change from the core fixture: Cara (member 3) borrows Poems (book 2),
-- which Ada already has on loan. Poems now has two active loans by two
-- different members, so it should join Algorithms in the threshold report,
-- and Cara is no longer a zero-match member.
PRAGMA foreign_keys = ON;
INSERT INTO loan VALUES (5, 3, 2, 0);
