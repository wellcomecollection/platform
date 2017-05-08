package uk.ac.wellcome.test.utils

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.xpack.security.XPackElasticClient
import org.elasticsearch.common.settings.Settings
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, Suite}
import uk.ac.wellcome.elasticsearch.mappings.WorksIndex

trait IndexedElasticSearchLocal
    extends ElasticSearchLocal
      with BeforeAndAfterEach { this: Suite =>

  val indexName = "records"
  val itemType = "item"

  override def beforeAll(): Unit = {
    super.beforeAll()

    if (elasticClient.execute(indexExists(indexName)).await.isExists){
      elasticClient.execute(deleteIndex(indexName)).await
    }
    new WorksIndex(elasticClient, indexName, itemType).create.await
  }

  override def beforeEach(): Unit = {
    elasticClient.execute(deleteIn(indexName).by(matchAllQuery())).await
    super.beforeEach()
  }
}
