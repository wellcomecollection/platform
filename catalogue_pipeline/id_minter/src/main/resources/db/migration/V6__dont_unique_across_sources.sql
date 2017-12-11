USE ${database};

ALTER TABLE ${tableName}
CHANGE COLUMN ontologyType OntologyType varchar(255) NOT NULL;

ALTER TABLE ${tableName}
CHANGE COLUMN CanonicalID CanonicalId varchar(255) NOT NULL;

ALTER TABLE ${tableName}
CHANGE COLUMN MiroID SourceId varchar(255) NOT NULL;

ALTER TABLE ${tableName}
ADD COLUMN SourceSystem varchar(255) NOT NULL DEFAULT 'miro-image-number';

ALTER TABLE ${tableName}
ALTER COLUMN SourceSystem DROP DEFAULT;

ALTER TABLE ${tableName}
DROP INDEX ontologyType_3;

ALTER TABLE ${tableName}
DROP INDEX ontologyType;

ALTER TABLE ${tableName}
DROP COLUMN SierraSystemNumber;

ALTER TABLE ${tableName}
ADD CONSTRAINT UniqueFromSource
UNIQUE KEY (OntologyType, SourceSystem, SourceId);