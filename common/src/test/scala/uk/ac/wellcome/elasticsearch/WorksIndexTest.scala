package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{indexInto, search}
import org.scalacheck.Arbitrary
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import org.scalacheck.ScalacheckShapeless._
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.elasticsearch.client.ResponseException
import org.scalatest.prop.PropertyChecks
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.test.utils.ExtendedPatience

class WorksIndexTest
    extends FunSpec
    with ElasticsearchFixtures
    with ScalaFutures
    with Eventually
    with ExtendedPatience
    with Matchers
    with JsonTestUtil
    with BeforeAndAfterEach
    with PropertyChecks {

  implicitly[Arbitrary[IdentifiedWork]]

  it("puts a valid work") {
    forAll { sampleWork: IdentifiedWork =>
      withLocalElasticsearchIndex("works", "work") { indexName =>
        val sampleWorkJson = JsonUtil.toJson(sampleWork).get

        eventually {
          for {
            _ <- elasticClient.execute(
              indexInto(indexName / "work").doc(sampleWorkJson))
            hits <- elasticClient
              .execute(search(s"$indexName/work").matchAllQuery())
              .map { _.hits.hits }
          } yield {
            hits should have size 1

            assertJsonStringsAreEqual(hits.head.sourceAsString, sampleWorkJson)
          }
        }
      }
    }
  }

  it("does not put an invalid work") {
    withLocalElasticsearchIndex("works", "work") { indexName =>
      val badTestObject = BadTestObject("id", 5)
      val badTestObjectJson = JsonUtil.toJson(badTestObject).get

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
