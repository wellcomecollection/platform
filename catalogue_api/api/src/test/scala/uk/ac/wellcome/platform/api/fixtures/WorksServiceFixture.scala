package uk.ac.wellcome.platform.api.fixtures

import org.scalatest.{Assertion, Suite}
import uk.ac.wellcome.platform.api.services.{ElasticsearchService, WorksService}
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait WorksServiceFixture { this: Suite =>
  def withWorksService(searchService: ElasticsearchService)(
    testWith: TestWith[WorksService, Assertion]) = {
    val worksService = new WorksService(
      apiConfig = ApiConfig(
        host = "example.org",
        scheme = "https",
        defaultPageSize = 10,
        pathPrefix = "/catalogue/works",
        contextSuffix = "/conext.json"
      ),
      searchService = searchService
    )
    testWith(worksService)
  }
}
