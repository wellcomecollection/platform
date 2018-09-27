package uk.ac.wellcome.elasticsearch.test.fixtures

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import org.elasticsearch.index.VersionType
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, Suite}
import uk.ac.wellcome.elasticsearch.{
  DisplayElasticConfig,
  ElasticClientBuilder,
  ElasticsearchIndex,
  WorksIndex
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

trait ElasticsearchFixtures
    extends Eventually
    with IntegrationPatience
    with ScalaFutures
    with Matchers
    with JsonAssertions { this: Suite =>

  private val esHost = "localhost"
  private val esPort = 9200

  def displayEsLocalFlags(indexNameV1: String,
                          indexNameV2: String,
                          itemType: String) =
    Map(
      "es.host" -> esHost,
      "es.port" -> esPort.toString,
      "es.index.v1" -> indexNameV1,
      "es.index.v2" -> indexNameV2,
      "es.type" -> itemType
    )

  def ingestEsLocalFlags(indexName: String, itemType: String) =
    Map(
      "es.host" -> esHost,
      "es.port" -> esPort.toString,
      "es.index" -> indexName,
      "es.type" -> itemType
    )

  val elasticClient: HttpClient = ElasticClientBuilder.create(
    hostname = esHost,
    port = esPort,
    protocol = "http",
    username = "elastic",
    password = "changeme"
  )

  // Elasticsearch takes a while to start up so check that it actually started before running tests
  eventually {
    elasticClient.execute(clusterHealth()).await.numberOfNodes shouldBe 1
  }

  def withLocalElasticsearchIndex[R](
    indexName: String = (Random.alphanumeric take 10 mkString) toLowerCase,
    itemType: String)(testWith: TestWith[String, R]): R = {

    val elasticConfig = DisplayElasticConfig(
      documentType = itemType,
      indexV1name = indexName,
      indexV2name = s"$indexName-v2"
    )

    val index = new WorksIndex(
      client = elasticClient,
      rootIndexType = elasticConfig.documentType
    )

    withLocalElasticsearchIndex(index, indexName)(testWith)
  }

  def withLocalElasticsearchIndex[R](
    index: ElasticsearchIndex,
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

  def assertElasticsearchEventuallyHasWork(indexName: String,
                                           itemType: String,
                                           works: IdentifiedBaseWork*) = {
    works.map { work =>
      val workJson = toJson(work).get

      eventually {
        val getResponse = elasticClient
          .execute(get(work.canonicalId).from(s"$indexName/$itemType"))
          .await

        getResponse.exists shouldBe true

        assertJsonStringsAreEqual(getResponse.sourceAsString, workJson)
      }
    }
  }

  def assertElasticsearchNeverHasWork(indexName: String,
                                      itemType: String,
                                      works: IdentifiedBaseWork*) = {
    // Let enough time pass to account for elasticsearch
    // eventual consistency before asserting
    Thread.sleep(500)

    works.foreach { work =>
      val hit = elasticClient
        .execute(get(work.canonicalId).from(s"$indexName/$itemType"))
        .await

      hit.found shouldBe false
    }
  }

  def insertIntoElasticsearch(indexName: String,
                              itemType: String,
                              works: IdentifiedBaseWork*) = {
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
