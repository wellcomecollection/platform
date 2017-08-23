-- This patch is preliminary work for supporting items in the ID minter.
--
-- Because a single MIRO record becomes both a work and an item, we:
--
--   * Add an ontologyType field so we can distinguish between an ID minted
--     for a work and an item.  Everything in the ID minter database when
--     we made this migration was a "Work", so we backfill existing rows,
--     then set it to NULL for future rows.

USE ${database};

ALTER TABLE ${tableName}
ADD COLUMN ontologyType varchar(255) NOT NULL DEFAULT 'Work' AFTER CanonicalID;

ALTER TABLE ${tableName}
ALTER COLUMN ontologyType DROP DEFAULT;

--   * Drop the uniqueness constraint on (MiroID), and add a new uniqueness
--     constraint for (ontologyType, MiroID).  This allows us to put the same
--     MiroID on multiple rows: once for the work, once for the item.

ALTER TABLE ${tableName}
DROP INDEX MiroID;

ALTER TABLE ${tableName}
ADD UNIQUE (ontologyType, MiroID);
