package uk.ac.wellcome.platform.reindex.creator.models

import uk.ac.wellcome.storage.dynamo.DynamoConfig

/** A request to identify all the shards in the table that need reindexing.
  *
  * @param dynamoConfig Table and index configuration for the table to reindex.
  * @param shardId Name of the shard to reindex.
  * @param desiredVersion Version to reindex everything in this shard to.
  */
case class ReindexJob(
  dynamoConfig: DynamoConfig,
  shardId: String,
  desiredVersion: Int
)
