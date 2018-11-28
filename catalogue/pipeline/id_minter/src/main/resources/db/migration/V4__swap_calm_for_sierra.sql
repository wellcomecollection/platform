-- Now we're ingesting Sierra records, we're actually using Sierra IDs in
-- the ID minter.

USE ${database};

ALTER TABLE ${tableName}
ADD COLUMN SierraSystemNumber varchar(255) AFTER CalmAltRefNo;

ALTER TABLE ${tableName}
ADD UNIQUE (ontologyType, SierraSystemNumber);
