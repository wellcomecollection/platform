-- This patch is another bit of preliminary work for supporting items in
-- the ID minter.
--
-- In particular, it adds a second column of source identifiers: CalmAltRefNo.
--
-- This allows us to develop and test ID minter support for working with
-- multiple source identifiers.

USE ${database};

ALTER TABLE ${tableName}
ADD COLUMN CalmAltRefNo varchar(255) AFTER MiroID;

ALTER TABLE ${tableName}
ADD UNIQUE (ontologyType, CalmAltRefNo);
