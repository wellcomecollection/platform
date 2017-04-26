package uk.ac.wellcome.test.utils

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.xpack.security.XPackElasticClient
import org.elasticsearch.common.settings.Settings
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, Suite}

trait ElasticSearchLocal
    extends BeforeAndAfterAll
    with BeforeAndAfterEach
    with Eventually
    with IntegrationPatience
    with Matchers { this: Suite =>
  private val settings = Settings
    .builder()
    .put("cluster.name", "wellcome")
    .put("xpack.security.user", "elastic:changeme")
    .build()

  val elasticClient =
    XPackElasticClient(settings, ElasticsearchClientUri("localhost", 9300))

  override def beforeAll(): Unit = {
    // Elastic search takes a while to start up so check that it actually started before running tests
    eventually {
      elasticClient
        .execute(
          clusterHealth()
        )
        .await
        .getNumberOfNodes shouldBe 1
    }

    if (!elasticClient.execute(indexExists("records")).await.isExists)
      elasticClient.execute(createIndex("records")).await

    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    elasticClient.execute(deleteIn("records").by(matchAllQuery())).await
    super.beforeEach()
  }
}
