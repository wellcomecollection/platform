package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{indexInto, search}
import org.scalacheck.Arbitrary
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.{ElasticSearchLocal, JsonTestUtil}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import org.scalacheck.ScalacheckShapeless._
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.elasticsearch.client.ResponseException
import org.scalatest.prop.PropertyChecks
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.JsonUtil

class WorksIndexTest
    extends FunSpec
    with ElasticSearchLocal
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonTestUtil
    with BeforeAndAfterEach
    with PropertyChecks {

  val indexName = "works"
  val itemType = "work"

  val worksIndex = new WorksIndex(elasticClient, indexName, itemType)

  override def beforeEach(): Unit = {
    ensureIndexDeleted(indexName)
  }

  implicitly[Arbitrary[IdentifiedWork]]

  it("puts a valid work") {

    forAll { sampleWork: IdentifiedWork =>
      ensureIndexDeleted(indexName)
      createAndWaitIndexIsCreated(worksIndex, indexName)

      val sampleWorkJson = JsonUtil.toJson(sampleWork).get

      elasticClient
        .execute(
          indexInto(indexName / itemType)
            .doc(sampleWorkJson))

      eventually {
        val hits = elasticClient
          .execute(search(s"$indexName/$itemType").matchAllQuery())
          .map {
            _.hits.hits
          }
          .await

        hits should have size 1

        assertJsonStringsAreEqual(hits.head.sourceAsString, sampleWorkJson)
      }
    }
  }

  it("does not put an invalid work") {
    createAndWaitIndexIsCreated(worksIndex, indexName)

    val badTestObject = BadTestObject("id", 5)
    val badTestObjectJson = JsonUtil.toJson(badTestObject).get

    val eventualIndexResponse = elasticClient
      .execute(
        indexInto(indexName / itemType)
          .doc(badTestObjectJson))

    whenReady(eventualIndexResponse.failed) { exception =>
      exception shouldBe a[ResponseException]
    }
  }

}
