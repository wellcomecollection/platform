package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.twitter.inject.Logging
import uk.ac.wellcome.platform.api.models.DisplayWork
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

@Singleton
class WorksService @Inject()(
  searchService: ElasticSearchService
) {

  def findWorkById(canonicalId: String): Future[Option[DisplayWork]] =
    searchService
      .findResultById(canonicalId)
      .map { result =>
        if (result.exists) Some(DisplayWork(result.original)) else None
      }

  def findWorks(): Future[Array[DisplayWork]] =
    searchService
      .findResults(sortByField = "canonicalId")
      .map { _.hits.map { DisplayWork(_) } }

  def searchWorks(query: String): Future[Array[DisplayWork]] =
    searchService
      .simpleStringQueryResults(query)
      .map { _.hits.map { DisplayWork(_) } }
}
