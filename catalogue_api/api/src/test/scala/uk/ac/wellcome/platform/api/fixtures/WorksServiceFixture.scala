package uk.ac.wellcome.platform.api.fixtures

import org.scalatest.{Assertion, Suite}
import uk.ac.wellcome.platform.api.services.{
  ElasticSearchService,
  WorksService
}
import uk.ac.wellcome.test.fixtures.TestWith

trait WorksServiceFixture { this: Suite =>
  def withWorksService(searchService: ElasticSearchService)(
      testWith: TestWith[WorksService, Assertion]) = {
    val worksService = new WorksService(
      defaultPageSize = 10,
      searchService = searchService
    )
    testWith(worksService)
  }
}
