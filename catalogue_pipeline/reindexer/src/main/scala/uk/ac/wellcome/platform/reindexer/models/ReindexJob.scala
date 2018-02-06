package uk.ac.wellcome.platform.reindexer.models

// A request to reindex a shard of the SourceData table, as received
// from the reindex-job-creator.
case class ReindexJob(
  shardId: String,
  desiredVersion: Int
)
