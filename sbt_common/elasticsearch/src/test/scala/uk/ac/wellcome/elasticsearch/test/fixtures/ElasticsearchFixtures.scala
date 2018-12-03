package uk.ac.wellcome.elasticsearch.test.fixtures

import com.sksamuel.elastic4s.VersionType.ExternalGte
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{ElasticClient, Response}
import org.scalactic.source.Position
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Assertion, Matchers, Suite}
import uk.ac.wellcome.elasticsearch.{DisplayElasticConfig, ElasticClientBuilder, ElasticsearchIndex, WorksIndex}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

trait ElasticsearchFixtures
    extends Eventually
    with ScalaFutures
    with Matchers
    with JsonAssertions
    with IntegrationPatience { this: Suite =>

  private val esHost = "localhost"
  private val esPort = 9200

  val documentType = "work"

  def displayEsLocalFlags(indexNameV1: String, indexNameV2: String) =
    Map(
      "es.host" -> esHost,
      "es.port" -> esPort.toString,
      "es.index.v1" -> indexNameV1,
      "es.index.v2" -> indexNameV2,
      "es.type" -> documentType
    )

  val elasticClient: ElasticClient = ElasticClientBuilder.create(
    hostname = esHost,
    port = esPort,
    protocol = "http",
    username = "elastic",
    password = "changeme"
  )

  // Elasticsearch takes a while to start up so check that it actually started
  // before running tests.
  eventually {
    elasticClient.execute(clusterHealth()).await.result.numberOfNodes shouldBe 1
  }(
    PatienceConfig(
      timeout = scaled(Span(40, Seconds)),
      interval = scaled(Span(150, Millis))
    ),
    implicitly[Position])

  def withLocalElasticsearchIndex[R](testWith: TestWith[String, R]): R = {
    val indexName = createIndexName

    val elasticConfig = createDisplayElasticConfigWith(
      indexV1name = indexName,
      indexV2name = s"$indexName-v2"
    )

    val index = new WorksIndex(
      client = elasticClient,
      indexName = indexName
    )

    withLocalElasticsearchIndex(index, indexName) { indexName =>
      testWith(indexName)
    }
  }

  def withLocalElasticsearchIndex[R](
    index: ElasticsearchIndex,
    indexName: String = createIndexName)(testWith: TestWith[String, R]): R = {

    index.create(indexName).await

    // Elasticsearch is eventually consistent, so the future
    // completing doesn't actually mean that the index exists yet
    eventually {
      elasticClient
        .execute(indexExists(indexName))
        .await
        .result
        .isExists should be(true)
    }

    try {
      testWith(indexName)
    } finally {
      elasticClient.execute(deleteIndex(indexName))
    }
  }

  def assertElasticsearchEventuallyHasWork(
    indexName: String,
    works: IdentifiedBaseWork*): Seq[Assertion] =
    works.map { work =>
      val workJson = toJson(work).get

      eventually {
        val response: Response[GetResponse] = elasticClient
          .execute(get(work.canonicalId).from(s"$indexName/$documentType"))
          .await

        response.result.exists shouldBe true

        assertJsonStringsAreEqual(response.result.sourceAsString, workJson)
      }
    }

  def assertElasticsearchNeverHasWork(indexName: String,
                                      works: IdentifiedBaseWork*): Unit = {
    // Let enough time pass to account for elasticsearch
    // eventual consistency before asserting
    Thread.sleep(500)

    works.foreach { work =>
      val response: Response[GetResponse] = elasticClient
        .execute(get(work.canonicalId).from(s"$indexName/$documentType"))
        .await

      response.result.found shouldBe false
    }
  }

  def insertIntoElasticsearch(indexName: String,
                              works: IdentifiedBaseWork*): Assertion = {
    val result = elasticClient.execute(
      bulk(
        works.map { work =>
          val jsonDoc = toJson(work).get

          indexInto(indexName / documentType)
            .version(work.version)
            .versionType(ExternalGte)
            .id(work.canonicalId)
            .doc(jsonDoc)
        }
      )
    )

    whenReady(result) { _ =>
      eventually {
        val response: Response[SearchResponse] = elasticClient
          .execute {
            search(indexName).matchAllQuery()
          }
          .await
        response.result.hits.total shouldBe works.size
      }
    }
  }

  def createDisplayElasticConfigWith(
    indexV1name: String,
    indexV2name: String): DisplayElasticConfig =
    DisplayElasticConfig(
      indexV1name = indexV1name,
      indexV2name = indexV2name
    )

  private def createIndexName: String =
    (Random.alphanumeric take 10 mkString) toLowerCase
}
