package uk.ac.wellcome.platform.reindex.reindex_worker.models

/** This is the request received from a caller who wants to trigger a reindexer.
  *
  * @param id The name of the reindex to run, e.g. "miro--reporting" or "sierra--catalogue".
  * @param parameters Parameters for the reindex.
  */
case class ReindexRequest(
  id: String,
  parameters: ReindexParameters
)
