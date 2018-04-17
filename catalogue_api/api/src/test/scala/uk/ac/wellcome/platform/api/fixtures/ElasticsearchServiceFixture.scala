package uk.ac.wellcome.platform.api.fixtures

import org.scalatest.{Assertion, Suite}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.platform.api.services.ElasticSearchService
import uk.ac.wellcome.test.fixtures.TestWith

trait ElasticsearchServiceFixture extends ElasticsearchFixtures {
  this: Suite =>
  def withElasticSearchService(indexName: String, itemType: String)(
    testWith: TestWith[ElasticSearchService, Assertion]) = {
    val searchService = new ElasticSearchService(
      documentType = itemType,
      elasticClient = elasticClient
    )
    testWith(searchService)
  }
}
