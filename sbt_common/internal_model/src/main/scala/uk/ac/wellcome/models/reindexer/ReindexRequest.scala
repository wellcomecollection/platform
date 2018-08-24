package uk.ac.wellcome.models.reindexer

/** A request to update a single record.
  *
  * This is sent to an SNS topic, and handled by another application.
  *
  * @param id ID of the record to update (e.g. "miro/A1234567")
  * @param tableName The name of the DynamoDB table where the record lives
  *                  (e.g. SourceData, vhs-sierra).
  */
case class ReindexRequest(
  id: String,
  tableName: String
)
