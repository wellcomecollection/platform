package uk.ac.wellcome.platform.ingestor.fixtures

import org.scalatest.Suite
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.platform.ingestor.services.WorkIndexer
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkIndexerFixtures extends ElasticsearchFixtures { this: Suite =>
  def withWorkIndexer[R](testWith: TestWith[WorkIndexer, R]): R = {
    val workIndexer = new WorkIndexer(elasticClient = elasticClient)
    testWith(workIndexer)
  }

  def withWorkIndexerFixtures[R](esType: String)(
    testWith: TestWith[WorkIndexer, R]): R =
    withWorkIndexer { workIndexer =>
      testWith(workIndexer)
    }
}
