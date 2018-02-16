package uk.ac.wellcome.platform.reindex_worker.models;

// A request to reindex a shard of the SourceData table, as received
// from the reindex-job-creator.
case class ReindexJob(
  shardId: String,
  desiredVersion: Int
)

case class CompletedReindexJob(
  shardId: String,
  completedReindexVersion: Int
)

case object CompletedReindexJob {
  def apply(reindexJob: ReindexJob): CompletedReindexJob =
    CompletedReindexJob(
      shardId = reindexJob.shardId,
      completedReindexVersion = reindexJob.desiredVersion
    )
}
