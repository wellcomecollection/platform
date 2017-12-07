-- Now we're ingesting Sierra records, we're actually using Sierra IDs in
-- the ID minter.  Since we're not doing Calm yet, we'll drop this column
-- for now.

USE ${database};

ALTER TABLE ${tableName}
DROP COLUMN CalmAltRefNo;

ALTER TABLE ${tableName}
ADD COLUMN SierraSystemNumber varchar(255) AFTER MiroID;

ALTER TABLE ${tableName}
ADD UNIQUE (ontologyType, SierraSystemNumber);
