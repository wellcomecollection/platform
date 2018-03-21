package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{indexInto, search}
import org.scalacheck.{Arbitrary, Shrink}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import org.scalacheck.ScalacheckShapeless._
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.elasticsearch.client.ResponseException
import org.scalatest.prop.PropertyChecks
import uk.ac.wellcome.utils.JsonUtil._

class WorksIndexTest
    extends FunSpec
    with ElasticsearchFixtures
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonTestUtil
    with BeforeAndAfterEach
    with ExtendedPatience
    with PropertyChecks {

  // On failure, scalacheck tries to shrink to the smallest input that causes a failure.
  // With IdentifiedWork, that means that it never actually completes.
  implicit val noShrink = Shrink.shrinkAny[IdentifiedWork]

  it("puts a valid work") {
    forAll { sampleWork: IdentifiedWork =>
      withLocalElasticsearchIndex(itemType = "work") { indexName =>
        val sampleWorkJson = toJson(sampleWork).get

        val futureIndexResponse = elasticClient.execute(
          indexInto(indexName / "work").doc(sampleWorkJson))

        whenReady(futureIndexResponse) { _ =>
          // Elasticsearch is eventually consistent so, when the future completes,
          // the documents might not immediately appear in search
          eventually {
            val hits = elasticClient
              .execute(search(s"$indexName/work").matchAllQuery())
              .map {
                _.hits.hits
              }
              .await

            hits should have size 1
            assertJsonStringsAreEqual(hits.head.sourceAsString, sampleWorkJson)
          }
        }
      }
    }
  }

  it("does not put an invalid work") {
    withLocalElasticsearchIndex(itemType = "work") { indexName =>
      val badTestObject = BadTestObject("id", 5)
      val badTestObjectJson = toJson(badTestObject).get

      val response = elasticClient.execute(
        indexInto(indexName / "work").doc(badTestObjectJson))

      whenReady(response.failed) { exception =>
        exception shouldBe a[ResponseException]
      }
    }
  }

}
