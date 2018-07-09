package uk.ac.wellcome.models.reindexer

/** A request to update a single record in SourceData to [[desiredVersion]].
  *
  * This is sent to an SNS topic, and handled by another application.
  *
  * @param id ID of the record to update (e.g. "miro/A1234567")
  * @param desiredVersion The reindex version to set on the record.
  */
case class ReindexRequest(
  id: String,
  desiredVersion: Int
)
