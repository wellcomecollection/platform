CREATE TABLE ${database}.${tableName} (
                 CanonicalID varchar(255),
                 MiroID varchar(255),
                 PRIMARY KEY (CanonicalID),
                 CONSTRAINT UNIQUE (MiroID));