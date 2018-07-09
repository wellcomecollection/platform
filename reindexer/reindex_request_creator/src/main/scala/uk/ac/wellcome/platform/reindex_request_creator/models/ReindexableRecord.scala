package uk.ac.wellcome.platform.reindex_request_creator.models

/** Represents a record in a source table which we might be able to reindex.
  *
  * @param id ID of the record.
  * @param reindexVersion Current reindex version in the table.
  */
case class ReindexableRecord(
  id: String,
  reindexVersion: Int
)
