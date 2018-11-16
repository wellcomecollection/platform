package uk.ac.wellcome.platform.reindex.reindex_worker.models

/** A request to reindex some or all of the records in a table.
  *
  * The two classes represent the two ways you might use the reindexer:
  *
  *   1) For a "complete" reindex -- every record in the table should be sent
  *      to the downstream applications.  Use this when you want to reprocess
  *      the entire data set.
  *   2) For a "partial" reindex -- when you want to test the downstream
  *      applications without swamping them with records.  Use this for smoke tests.
  *
  */
sealed trait ReindexParameters

case class CompleteReindexParameters(
  segment: Int,
  totalSegments: Int
) extends ReindexParameters

case class PartialReindexParameters(
  maxRecords: Int
) extends ReindexParameters
