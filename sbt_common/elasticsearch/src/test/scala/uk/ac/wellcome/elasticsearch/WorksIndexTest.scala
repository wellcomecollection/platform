package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{indexInto, search}
import org.scalacheck.Arbitrary
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.test.utils.JsonTestUtil
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
    with PropertyChecks {

  implicitly[Arbitrary[IdentifiedWork]]

  it("puts a valid work") {
    withLocalElasticsearchIndex(itemType = "work") {
      indexName =>
      forAll { sampleWork: IdentifiedWork =>
        val sampleWorkJson = toJson(sampleWork).get

        val eventualHits =
          for {
            _ <- elasticClient.execute(
              indexInto(indexName / "work").doc(sampleWorkJson))
            hits <- elasticClient
              .execute(search(s"$indexName/work").matchAllQuery())
              .map { _.hits.hits }
          } yield hits

        whenReady(eventualHits) { hits =>
           hits should have size 1
           assertJsonStringsAreEqual(hits.head.sourceAsString, sampleWorkJson)
        }
      }
    }
  }

  it("does not put an invalid work") {
    withLocalElasticsearchIndex(itemType = "work") { indexName =>
      val badTestObject = BadTestObject("id", 5)
      val badTestObjectJson = toJson(badTestObject).get

      val eventualResponse =
        for {
          response <- elasticClient.execute(
            indexInto(indexName / "work").doc(badTestObjectJson))
        } yield response

      whenReady(eventualResponse.failed) { exception =>
        exception shouldBe a[ResponseException]
      }
    }
  }

}
