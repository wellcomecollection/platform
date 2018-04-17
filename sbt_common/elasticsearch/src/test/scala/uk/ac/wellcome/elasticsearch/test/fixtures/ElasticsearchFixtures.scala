package uk.ac.wellcome.elasticsearch.test.fixtures

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.index.IndexResponse
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.index.VersionType
import org.scalatest.{Matchers, Suite}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.elasticsearch.WorksIndex
import uk.ac.wellcome.elasticsearch.finatra.modules.ElasticCredentials
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.elasticsearch.ElasticSearchIndex

import scala.concurrent.Future
import scala.util.Random

trait ElasticsearchFixtures
    extends Eventually
    with ExtendedPatience
    with ScalaFutures
    with Matchers
    with JsonTestUtil { this: Suite =>

  private val esHost = "localhost"
  private val esPort = "9200"
  private val esName = "wellcome"

  def esLocalFlags(indexNameV1: String,
                   indexNameV2: String,
                   itemType: String) = Map(
    "es.host" -> esHost,
    "es.port" -> esPort,
    "es.name" -> esName,
    "es.index.v1" -> indexNameV1,
    "es.index.v2" -> indexNameV2,
    "es.type" -> itemType
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

  def withLocalElasticsearchIndex[R](indexName: String = (Random.alphanumeric take 10 mkString) toLowerCase,itemType: String)(testWith: TestWith[String, R]): R = {

    val index = new WorksIndex(
      client = elasticClient,
      itemType = itemType
    )

    withLocalElasticsearchIndex(index, indexName)(testWith)
  }

  def withLocalElasticsearchIndex[R](index: ElasticSearchIndex, indexName: String)(
    testWith: TestWith[String, R]): R = {

    index.create(indexName).await

    // Elasticsearch is eventually consistent, so the future
    // completing doesn't actually mean that the index exists yet
    eventually {
      elasticClient
        .execute(indexExists(indexName))
        .await
        .isExists should be(true)
    }

    try {
      testWith(indexName)
    } finally {
      elasticClient.execute(deleteIndex(indexName))
    }
  }

  def assertElasticsearchEventuallyHasWork(work: IdentifiedWork,
                                           indexName: String,
                                           itemType: String) = {
    val workJson = toJson(work).get

    eventually {
      val hits = elasticClient
        .execute(search(s"$indexName/$itemType").matchAllQuery().limit(100))
        .map { _.hits.hits }
        .await

      hits should have size 1

      assertJsonStringsAreEqual(hits.head.sourceAsString, workJson)
    }
  }

  def insertIntoElasticsearch(indexName: String,
                              itemType: String,
                              works: IdentifiedWork*) = {
    works.foreach { work =>
      val jsonDoc = toJson(work).get

      val result: Future[IndexResponse] = elasticClient.execute(
        indexInto(indexName / itemType)
          .version(work.version)
          .versionType(VersionType.EXTERNAL_GTE)
          .id(work.canonicalId)
          .doc(jsonDoc)
      )

      result.map { indexResponse =>
        print(indexResponse)
      }
    }
    eventually {
      elasticClient
        .execute {
          search(indexName).matchAllQuery()
        }
        .await
        .hits should have size works.size
    }
  }
}
