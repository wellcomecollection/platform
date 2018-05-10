package uk.ac.wellcome.elasticsearch.test.fixtures

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.index.VersionType
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Matchers, Suite}
import uk.ac.wellcome.elasticsearch.finatra.modules.ElasticClientConfig
import uk.ac.wellcome.elasticsearch.{ElasticSearchIndex, WorksIndex}
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

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
    .setHttpClientConfigCallback(new ElasticClientConfig("elastic", "changeme"))
    .build()

  val elasticClient: HttpClient = HttpClient.fromRestClient(restClient)

  // Elasticsearch takes a while to start up so check that it actually started before running tests
  eventually {
    elasticClient.execute(clusterHealth()).await.numberOfNodes shouldBe 1
  }

  def withLocalElasticsearchIndex[R](
    indexName: String = (Random.alphanumeric take 10 mkString) toLowerCase,
    itemType: String)(testWith: TestWith[String, R]): R = {

    val index = new WorksIndex(
      client = elasticClient,
      itemType = itemType
    )

    withLocalElasticsearchIndex(index, indexName)(testWith)
  }

  def withLocalElasticsearchIndex[R](
    index: ElasticSearchIndex,
    indexName: String)(testWith: TestWith[String, R]): R = {

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

  def assertElasticsearchNeverHasWork(work: IdentifiedWork,
                                      indexName: String,
                                      itemType: String) = {
    // Let enough time pass to account for elasticsearch
    // eventual consistency before asserting
    Thread.sleep(500)

    val hit = elasticClient
      .execute(get(work.canonicalId).from(s"$indexName/$itemType"))
      .await

    hit.found shouldBe false
  }

  def insertIntoElasticsearch(indexName: String,
                              itemType: String,
                              works: IdentifiedWork*) = {
    val result = elasticClient.execute(
      bulk(
        works.map { work =>
          val jsonDoc = toJson(work).get

          indexInto(indexName / itemType)
            .version(work.version)
            .versionType(VersionType.EXTERNAL_GTE)
            .id(work.canonicalId)
            .doc(jsonDoc)
        }
      )
    )

    whenReady(result) { _ =>
      eventually {
        val hits = elasticClient
          .execute {
            search(indexName).matchAllQuery()
          }
          .await
          .hits
        hits.total shouldBe works.size
      }
    }
  }
}
