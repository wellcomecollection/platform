package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import org.elasticsearch.client.ResponseException
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.utils.JsonAssertions

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

case class TestObject(
  id: String,
  description: String,
  visible: Boolean
)

case class CompatibleTestObject(
  id: String,
  description: String,
  count: Int,
  visible: Boolean
)

case class BadTestObject(
  id: String,
  weight: Int
)

class ElasticsearchIndexTest
    extends FunSpec
    with ElasticsearchFixtures
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonAssertions
    with BeforeAndAfterEach {

  val testType = "thing"

  object TestIndex extends ElasticsearchIndex {
    val httpClient: HttpClient = elasticClient
    val mappingDefinition: MappingDefinition = mapping(testType)
      .dynamic(DynamicMapping.Strict)
      .as(
        keywordField("id"),
        textField("description"),
        booleanField("visible")
      )

    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  }

  object CompatibleTestIndex extends ElasticsearchIndex {
    val httpClient: HttpClient = elasticClient
    val mappingDefinition: MappingDefinition = mapping(testType)
      .dynamic(DynamicMapping.Strict)
      .as(
        keywordField("id"),
        textField("description"),
        intField("count"),
        booleanField("visible")
      )

    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  }

  it("creates an index into which doc of the expected type can be put") {
    withLocalElasticsearchIndex(TestIndex) { indexName =>
      val testObject = TestObject("id", "description", true)
      val testObjectJson = toJson(testObject).get

      eventually {
        for {
          _ <- elasticClient.execute(
            indexInto(indexName / testType).doc(testObjectJson))
          hits <- elasticClient
            .execute(search(s"$indexName/$testType").matchAllQuery())
            .map { _.hits.hits }
        } yield {
          hits should have size 1

          assertJsonStringsAreEqual(hits.head.sourceAsString, testObjectJson)
        }
      }
    }
  }

  it("create an index where inserting a doc of an unexpected type fails") {
    withLocalElasticsearchIndex(TestIndex) { indexName =>
      val badTestObject = BadTestObject("id", 5)
      val badTestObjectJson = toJson(badTestObject).get

      val eventuallyResponse =
        for {
          response <- elasticClient.execute(
            indexInto(indexName / testType).doc(badTestObjectJson))
        } yield response

      whenReady(eventuallyResponse.failed) { exception =>
        exception shouldBe a[ResponseException]
      }
    }
  }

  it("updates an already existing index with a compatible mapping") {
    withLocalElasticsearchIndex(TestIndex) { indexName =>
      withLocalElasticsearchIndex(CompatibleTestIndex, indexName = indexName) {
        testIndexName =>
          val compatibleTestObject =
            CompatibleTestObject("id", "description", 5, visible = true)
          val compatibleTestObjectJson = toJson(compatibleTestObject).get

          val futureInsert = elasticClient.execute(
            indexInto(testIndexName / testType) doc compatibleTestObjectJson)

          whenReady(futureInsert) { _ =>
            eventually {
              val hits = elasticClient
                .execute(search(s"$testIndexName/$testType").matchAllQuery())
                .map { _.hits.hits }
                .await

              hits should have size 1

              assertJsonStringsAreEqual(
                hits.head.sourceAsString,
                compatibleTestObjectJson
              )
            }
          }
      }
    }
  }
}
