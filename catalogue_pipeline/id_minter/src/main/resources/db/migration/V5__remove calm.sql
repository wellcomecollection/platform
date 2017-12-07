-- Now we're ingesting Sierra records, we're actually using Sierra IDs in
-- the ID minter.

USE ${database};

-- Mysql automatically sets the index name to the name of the first
-- field and appends a counter if there are more than one index on the same field.
-- This resolves to the unique constarint on ontologyType and CalmAltRefNo.
-- Next time remember to name the constraints
ALTER TABLE ${tableName} DROP INDEX ontologyType_2;

ALTER TABLE ${tableName} DROP COLUMN CalmAltRefNo;
