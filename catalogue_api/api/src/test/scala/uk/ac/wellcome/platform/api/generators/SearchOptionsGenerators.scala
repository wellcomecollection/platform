package uk.ac.wellcome.platform.api.generators

import uk.ac.wellcome.platform.api.models.WorkFilter
import uk.ac.wellcome.platform.api.services.{
  ElasticsearchQueryOptions,
  WorksSearchOptions
}

trait SearchOptionsGenerators {
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

  def createElasticsearchQueryOptions: ElasticsearchQueryOptions =
    createElasticsearchQueryOptionsWith()

  def createWorksSearchOptionsWith(
    filters: List[WorkFilter] = List(),
    pageSize: Int = 10,
    pageNumber: Int = 1
  ): WorksSearchOptions =
    WorksSearchOptions(
      filters = filters,
      pageSize = pageSize,
      pageNumber = pageNumber
    )

  def createWorksSearchOptions: WorksSearchOptions =
    createWorksSearchOptionsWith()
}
