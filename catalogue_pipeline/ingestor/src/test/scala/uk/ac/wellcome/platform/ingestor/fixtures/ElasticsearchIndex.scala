package uk.ac.wellcome.platform.ingestor.fixtures

import com.sksamuel.elastic4s.http.ElasticDsl.{clusterHealth, deleteIndex, indexExists}
import com.sksamuel.elastic4s.http.HttpClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.scalatest.{Matchers, Suite}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import uk.ac.wellcome.elasticsearch.WorksIndex
import uk.ac.wellcome.finatra.modules.ElasticCredentials
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.util.{Random, Try}

trait ElasticSearchIndex
    extends Eventually
    with ExtendedPatience
    with ScalaFutures
    with Matchers { this: Suite =>

  private val esHost = "localhost"
  private val esPort = "9200"
  private val esName = "wellcome"

  val esLocalFlags = Map(
    "es.host" -> esHost,
    "es.port" -> esPort,
    "es.name" -> esName
  )

  val restClient: RestClient = RestClient
    .builder(new HttpHost("localhost", 9200, "http"))
    .setHttpClientConfigCallback(new ElasticCredentials("elastic", "changeme"))
    .build()

  val elasticClient: HttpClient = HttpClient.fromRestClient(restClient)

  // Elasticsearch takes a while to start up so check that it actually started before running tests
  eventually {
    elasticClient.execute(clusterHealth()).await.numberOfNodes shouldBe 1
  }

  def withLocalElasticIndex[R](testWith: TestWith[String, R]) = {
    val indexName = Random.alphanumeric take 10 mkString
    val itemType = "works"
    val index = new WorksIndex(
      client = elasticClient,
      name = indexName,
      itemType = itemType
    )

    index.create.map { _ =>
      elasticClient.execute(indexExists(indexName)).await.isExists should be(true)
    }.await

    try {
      testWith(indexName)
    } finally {
      Try { elasticClient.execute(deleteIndex(indexName)) }
    }
  }
}