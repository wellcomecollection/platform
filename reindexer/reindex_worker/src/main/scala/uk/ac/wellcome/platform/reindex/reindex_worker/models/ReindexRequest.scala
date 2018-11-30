package uk.ac.wellcome.platform.reindex.reindex_worker.models

/** The main interface to the reindexer.
  *
  * It receives JSON-encoded instances of this case class from SNS, and uses
  * them to decide what sort of reindex to run.
  *
  * @param jobConfigId The name of the reindex to run, e.g. "miro--reporting" or "sierra--catalogue".
  * @param parameters Parameters for the reindex.
  */
case class ReindexRequest(
  jobConfigId: String,
  parameters: ReindexParameters
)
