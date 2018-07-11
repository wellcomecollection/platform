package uk.ac.wellcome.models.reindexer

import uk.ac.wellcome.models.Id

/** Represents a record in a source table which we might be able to reindex.
  *
  * @param id ID of the record.
  * @param reindexVersion Current reindex version in the table.
  */
case class ReindexableRecord(
  id: String,
  version: Int,
  reindexVersion: Int
) extends Id
