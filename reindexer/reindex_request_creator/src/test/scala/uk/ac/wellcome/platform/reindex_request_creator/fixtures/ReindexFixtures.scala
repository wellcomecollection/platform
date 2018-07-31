package uk.ac.wellcome.platform.reindex_request_creator.fixtures

import uk.ac.wellcome.platform.reindex_request_creator.models.ReindexJob
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

  def createReindexJobWith(
    dynamoConfig: DynamoConfig,
    shardId: String = "sierra/123",
    desiredVersion: Int = 5
  ): ReindexJob = ReindexJob(
    dynamoConfig = dynamoConfig,
    shardId = shardId,
    desiredVersion = desiredVersion
  )
}
