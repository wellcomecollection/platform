package uk.ac.wellcome.platform.api.fixtures

import org.scalatest.Suite
import uk.ac.wellcome.platform.api.services.{ElasticsearchService, WorksService}
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait WorksServiceFixture { this: Suite =>
  def withWorksService[R](searchService: ElasticsearchService)(
    testWith: TestWith[WorksService, R]): R = {
    val worksService = new WorksService(searchService = searchService)
    testWith(worksService)
  }
}
