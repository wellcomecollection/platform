package uk.ac.wellcome.platform.api.generators

import uk.ac.wellcome.platform.api.models.WorkFilter
import uk.ac.wellcome.platform.api.services.{ElasticsearchDocumentOptions, ElasticsearchQueryOptions}

trait SearchOptionsGenerators {
  val itemType: String

  def createElasticsearchDocumentOptionsWith(indexName: String): ElasticsearchDocumentOptions =
    ElasticsearchDocumentOptions(
      indexName = indexName,
      documentType = itemType
    )

  def createElasticsearchQueryOptionsWith(
    filters: List[WorkFilter] = List(),
    limit: Int = 10,
    from: Int = 0
  ): ElasticsearchQueryOptions =
    ElasticsearchQueryOptions(
      filters = filters,
      limit = limit,
      from = from
    )

  def createElasticsearchQueryOptions: ElasticsearchQueryOptions = createElasticsearchQueryOptionsWith()
}
