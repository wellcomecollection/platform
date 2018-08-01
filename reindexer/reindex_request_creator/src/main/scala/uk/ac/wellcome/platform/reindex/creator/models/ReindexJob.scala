package uk.ac.wellcome.platform.reindex.creator.models

/** A request to identify all the shards in the table that need reindexing.
  *
  * @param shardId Name of the shard to reindex.
  * @param desiredVersion Version to reindex everything in this shard to.
  */
case class ReindexJob(
  shardId: String,
  desiredVersion: Int
)
