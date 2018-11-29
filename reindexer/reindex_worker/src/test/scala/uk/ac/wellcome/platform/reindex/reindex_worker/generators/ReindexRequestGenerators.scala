package uk.ac.wellcome.platform.reindex.reindex_worker.generators

import uk.ac.wellcome.platform.reindex.reindex_worker.models.{ReindexParameters, ReindexRequest}

trait ReindexRequestGenerators {
  def createReindexRequest(parameters: ReindexParameters): ReindexRequest =
    ReindexRequest(
      id = "TBC",
      parameters = parameters
    )
}
