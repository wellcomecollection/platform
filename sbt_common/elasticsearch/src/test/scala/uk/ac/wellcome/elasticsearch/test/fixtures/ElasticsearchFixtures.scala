package uk.ac.wellcome.elasticsearch.test.fixtures

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.VersionType.ExternalGte
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.cluster.ClusterHealthResponse
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.index.admin.IndexExistsResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{ElasticClient, Response}
import org.scalactic.source.Position
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Assertion, Matchers, Suite}
import uk.ac.wellcome.elasticsearch._
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

  def displayEsLocalFlags(indexV1: Index, indexV2: Index): Map[String, String] =
    Map(
      "es.host" -> esHost,
      "es.port" -> esPort.toString,
      "es.index.v1" -> indexV1.name,
      "es.index.v2" -> indexV2.name,
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
    val response: Response[ClusterHealthResponse] = elasticClient
      .execute(clusterHealth())
      .await

    response.result.numberOfNodes shouldBe 1
  }(
    PatienceConfig(
      timeout = scaled(Span(40, Seconds)),
      interval = scaled(Span(150, Millis))
    ),
    implicitly[Position])

  def withLocalWorksIndex[R](testWith: TestWith[String, R]): R =
    withLocalElasticsearchIndex[R](WorksIndex) { indexName =>
      testWith(indexName)
    }

  def withLocalWorksIndex2[R](testWith: TestWith[Index, R]): R =
    withLocalElasticsearchIndex[R](WorksIndex) { indexName =>
      testWith(Index(name = indexName))
    }

  private val elasticsearchIndexCreator = new ElasticsearchIndexCreator(
    elasticClient = elasticClient
  )

  def withLocalElasticsearchIndex[R](
    index: MappingDefinitionBuilder,
    indexName: String = createIndexName)(testWith: TestWith[String, R]): R =
    withLocalElasticsearchIndex2(index, Index(indexName)) { idx =>
      testWith(idx.name)
    }

  def withLocalElasticsearchIndex2[R](
    mappingDefinition: MappingDefinitionBuilder,
    index: Index = createIndexName)(testWith: TestWith[Index, R]): R = {
    elasticsearchIndexCreator
      .create(
        index = index,
        mappingDefinition = mappingDefinition.buildMappingDefinition(documentType)
      )
      .await

    // Elasticsearch is eventually consistent, so the future
    // completing doesn't actually mean that the index exists yet
    eventuallyIndexExists(index)

    try {
      testWith(index)
    } finally {
      elasticClient.execute(deleteIndex(index.name))
    }
  }

  def eventuallyIndexExists(index: Index): Assertion =
    eventually {
      val response: Response[IndexExistsResponse] =
        elasticClient
          .execute(indexExists(index.name))
          .await

      response.result.isExists shouldBe true
    }

  def eventuallyDeleteIndex(index: Index): Assertion = {
    elasticClient.execute(deleteIndex(index.name))

    eventually {
      val response: Response[IndexExistsResponse] =
        elasticClient
          .execute(indexExists(index.name))
          .await

      response.result.isExists shouldBe false
    }
  }

  def assertElasticsearchEventuallyHasWork(
    indexName: String,
    works: IdentifiedBaseWork*): Seq[Assertion] =
    assertElasticsearchEventuallyHasWork2(Index(indexName), works: _*)

  def assertElasticsearchEventuallyHasWork2(
    index: Index,
    works: IdentifiedBaseWork*): Seq[Assertion] =
    works.map { work =>
      val workJson = toJson(work).get

      eventually {
        val response: Response[GetResponse] = elasticClient
          .execute(get(work.canonicalId).from(index.name / documentType))
          .await

        val getResponse = response.result

        getResponse.exists shouldBe true

        assertJsonStringsAreEqual(getResponse.sourceAsString, workJson)
      }
    }

  def assertElasticsearchNeverHasWork(indexName: String,
                                      works: IdentifiedBaseWork*): Unit =
    assertElasticsearchNeverHasWork2(Index(indexName), works: _*)

  def assertElasticsearchNeverHasWork2(index: Index,
                                       works: IdentifiedBaseWork*): Unit = {
    // Let enough time pass to account for elasticsearch
    // eventual consistency before asserting
    Thread.sleep(500)

    works.foreach { work =>
      val response: Response[GetResponse] = elasticClient
        .execute(get(work.canonicalId).from(index.name / documentType))
        .await

      response.result.found shouldBe false
    }
  }

  def insertIntoElasticsearch(indexName: String,
                              works: IdentifiedBaseWork*): Assertion =
    insertIntoElasticsearch(Index(name), works: _*)

  def insertIntoElasticsearch(index: Index,
                              works: IdentifiedBaseWork*): Assertion = {
    val result = elasticClient.execute(
      bulk(
        works.map { work =>
          val jsonDoc = toJson(work).get

          indexInto(index.name / documentType)
            .version(work.version)
            .versionType(ExternalGte)
            .id(work.canonicalId)
            .doc(jsonDoc)
        }
      )
    )

    whenReady(result) { _ =>
      eventually {
        val response: Response[SearchResponse] = elasticClient.execute {
          search(index).matchAllQuery()
        }.await

        response.result.hits.total shouldBe works.size
      }
    }
  }

  def createDisplayElasticConfigWith(
    indexV1: Index,
    indexV2: Index): DisplayElasticConfig =
    DisplayElasticConfig(
      documentType = documentType,
      indexV1 = indexV1,
      indexV2 = indexV2
    )

  private def createIndexName: String =
    (Random.alphanumeric take 10 mkString) toLowerCase
}
