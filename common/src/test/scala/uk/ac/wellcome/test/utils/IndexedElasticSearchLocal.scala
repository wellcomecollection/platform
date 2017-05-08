package uk.ac.wellcome.test.utils

import com.sksamuel.elastic4s.ElasticDsl._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import uk.ac.wellcome.elasticsearch.mappings.WorksIndex
import uk.ac.wellcome.utils.GlobalExecutionContext.context

trait IndexedElasticSearchLocal
    extends ElasticSearchLocal
    with BeforeAndAfterEach
    with BeforeAndAfterAll { this: Suite =>

  val indexName = "records"
  val itemType = "item"

  override def beforeAll(): Unit = {
    elasticClient
      .execute(indexExists(indexName))
      .map { result =>
        if (result.isExists) elasticClient.execute(deleteIndex(indexName))
      }

    eventually {
      elasticClient
        .execute(indexExists(indexName)).await.isExists should be (false)
    }
    new WorksIndex(elasticClient, indexName, itemType).create.await
  }

  override def beforeEach(): Unit = {
    elasticClient.execute(deleteIn(indexName).by(matchAllQuery())).await
    super.beforeEach()
  }
}
