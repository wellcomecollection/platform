package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import org.elasticsearch.client.ResponseException
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures

import uk.ac.wellcome.utils.GlobalExecutionContext.context

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

class ElasticSearchIndexTest
    extends FunSpec
    with ElasticsearchFixtures
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonTestUtil
    with BeforeAndAfterEach {

  val testType = "thing"

  object TestIndex extends ElasticSearchIndex {

    override val httpClient: HttpClient = elasticClient
    override val indexName = "test_index"
    override val mappingDefinition = mapping(testType)
      .dynamic(DynamicMapping.Strict)
      .as(
        keywordField("id"),
        textField("description"),
        booleanField("visible")
      )
  }

  object CompatibleTestIndex extends ElasticSearchIndex {

    override val httpClient = elasticClient
    override val indexName = "test_index"
    override val mappingDefinition = mapping(testType)
      .dynamic(DynamicMapping.Strict)
      .as(
        keywordField("id"),
        textField("description"),
        intField("count"),
        booleanField("visible")
      )
  }

  it("creates an index into which doc of the expected type can be put") {
    withLocalElasticsearchIndex(TestIndex) { indexName =>
      val testObject = TestObject("id", "description", true)
      val testObjectJson = JsonUtil.toJson(testObject).get

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
      val badTestObjectJson = JsonUtil.toJson(badTestObject).get

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
    withLocalElasticsearchIndex(TestIndex) { _ =>
      withLocalElasticsearchIndex(CompatibleTestIndex) { testIndexName =>
        val compatibleTestObject =
          CompatibleTestObject("id", "description", 5, visible = true)
        val compatibleTestObjectJson =
          JsonUtil.toJson(compatibleTestObject).get

        val futureInsert = elasticClient.execute(
              indexInto(testIndexName / testType) doc compatibleTestObjectJson)

        whenReady(futureInsert) { _ =>
          eventually {
            val hits = elasticClient
              .execute(search(s"$testIndexName/$testType").matchAllQuery())
              .map { _.hits.hits }.await

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
