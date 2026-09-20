-- Runs on every application startup (see spring.sql.init in application.yml), before
-- Hibernate's own ddl-auto:update schema check. Every statement here must stay idempotent
-- (safe to run against a database that already has the fix applied, and safe to run against
-- a brand new empty database) since there's no migration-version bookkeeping - this file is
-- the append-only history of one-off fixes Hibernate couldn't apply automatically on its own.
--
-- Statements are terminated with "//" (see spring.sql.init.separator), not ";" - Spring's
-- script splitter doesn't understand Postgres's $$ dollar-quoting and would otherwise cut a
-- DO $$ ... $$ block apart at the first ";" inside it.

-- Added when Edition gained a "type" column (FESTIVAL vs KNEIPENKONZERT): the old
-- uniqueness constraint on festival_year no longer applies once several Kneipenkonzerte can
-- share a year, and Hibernate can't add a NOT NULL column with no default to a table that
-- already has rows, so both steps have to happen here instead of via ddl-auto.
DO $$
DECLARE
    con_name text;
BEGIN
    IF to_regclass('editions') IS NULL THEN
        RETURN;
    END IF;

    SELECT conname INTO con_name
    FROM pg_constraint
    WHERE conrelid = 'editions'::regclass
      AND contype = 'u'
      AND conkey = (
          SELECT array_agg(attnum ORDER BY attnum)
          FROM pg_attribute
          WHERE attrelid = 'editions'::regclass AND attname = 'festival_year'
      );
    IF con_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE editions DROP CONSTRAINT %I', con_name);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'editions' AND column_name = 'type'
    ) THEN
        ALTER TABLE editions ADD COLUMN type varchar(255) NOT NULL DEFAULT 'FESTIVAL';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'editions_type_check') THEN
        ALTER TABLE editions ADD CONSTRAINT editions_type_check CHECK (type IN ('FESTIVAL', 'KNEIPENKONZERT'));
    END IF;
END $$//
