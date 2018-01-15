package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.PutMappingDefinition
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import org.elasticsearch.client.ResponseException
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Assertion, BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.test.utils.{ElasticSearchLocal, JsonTestUtil}
import uk.ac.wellcome.utils.JsonUtil
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
    with ElasticSearchLocal
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonTestUtil
    with BeforeAndAfterEach {

  val testIndexName = "test_index"
  val testType = "thing"

  override def beforeEach(): Unit = {
    ensureIndexDeleted(testIndexName)
  }

  class TestIndex extends ElasticSearchIndex {

    val httpClient: HttpClient = elasticClient
    val indexName: String = testIndexName

    val mappingDefinition = mapping(testType)
      .dynamic(DynamicMapping.Strict)
      .as(
        keywordField("id"),
        textField("description"),
        booleanField("visible")
      )
  }

  class CompatibleTestIndex extends ElasticSearchIndex {

    val httpClient: HttpClient = elasticClient
    val indexName: String = testIndexName

    val mappingDefinition = mapping(testType)
      .dynamic(DynamicMapping.Strict)
      .as(
        keywordField("id"),
        textField("description"),
        intField("count"),
        booleanField("visible")
      )
  }

  val testIndex = new TestIndex()
  val compatibleTestIndex = new CompatibleTestIndex()

  it("creates an index into which doc of the expected type can be put") {
    createAndWaitIndexIsCreated(testIndex, testIndexName)

    val testObject = TestObject("id", "description", true)
    val testObjectJson = JsonUtil.toJson(testObject).get

    elasticClient
      .execute(
        indexInto(testIndexName / testType)
          .doc(testObjectJson))

    eventually {
      val hits = elasticClient
        .execute(search(s"$testIndexName/$testType").matchAllQuery())
        .map {
          _.hits.hits
        }
        .await
      hits should have size 1

      assertJsonStringsAreEqual(hits.head.sourceAsString, testObjectJson)
    }
  }

  it("create an index where inserting a doc of an unexpected type fails") {
    createAndWaitIndexIsCreated(testIndex, testIndexName)

    val badTestObject = BadTestObject("id", 5)
    val badTestObjectJson = JsonUtil.toJson(badTestObject).get

    val eventualIndexResponse = elasticClient
      .execute(
        indexInto(testIndexName / testType)
          .doc(badTestObjectJson))

    whenReady(eventualIndexResponse.failed) { exception =>
      exception shouldBe a[ResponseException]
    }
  }

  it("updates an already existing index with a compatible mapping") {
    createAndWaitIndexIsCreated(testIndex, testIndexName)

    createAndWaitIndexIsCreated(compatibleTestIndex, testIndexName)

    val compatibleTestObject =
      CompatibleTestObject("id", "description", 5, true)
    val compatibleTestObjectJson = JsonUtil.toJson(compatibleTestObject).get

    elasticClient
      .execute(
        indexInto(testIndexName / testType)
          .doc(compatibleTestObjectJson))

    eventually {
      val hits = elasticClient
        .execute(search(s"$testIndexName/$testType").matchAllQuery())
        .map {
          _.hits.hits
        }
        .await
      hits should have size 1

      assertJsonStringsAreEqual(
        hits.head.sourceAsString,
        compatibleTestObjectJson
      )
    }
  }
}
