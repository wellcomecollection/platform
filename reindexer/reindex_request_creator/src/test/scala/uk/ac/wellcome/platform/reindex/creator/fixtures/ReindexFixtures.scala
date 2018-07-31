package uk.ac.wellcome.platform.reindex.creator.fixtures

import uk.ac.wellcome.platform.reindex.creator.models.ReindexJob
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table

trait ReindexFixtures {
  def createReindexJobWith(
    table: Table,
    shardId: String = "sierra/123",
    desiredVersion: Int = 5
  ): ReindexJob = ReindexJob(
    dynamoConfig = DynamoConfig(
      table = table.name,
      index = table.index
    ),
    shardId = shardId,
    desiredVersion = desiredVersion
  )

  def createReindexJobWith(dynamoConfig: DynamoConfig): ReindexJob = ReindexJob(
    dynamoConfig = dynamoConfig,
    shardId = "sierra/123",
    desiredVersion = 5
  )
}
