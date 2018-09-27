package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{indexInto, search, _}
import org.elasticsearch.client.ResponseException
import org.scalacheck.Shrink
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, Person, Subject, Unidentifiable}
import org.scalacheck.ScalacheckShapeless._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.work.test.util.WorksGenerators

import scala.concurrent.ExecutionContext.Implicits.global

class WorksIndexTest
    extends FunSpec
    with ElasticsearchFixtures
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonAssertions
    with BeforeAndAfterEach
    with IntegrationPatience
    with PropertyChecks with WorksGenerators {

  // On failure, scalacheck tries to shrink to the smallest input that causes a failure.
  // With IdentifiedWork, that means that it never actually completes.
  implicit val noShrink = Shrink.shrinkAny[IdentifiedBaseWork]

  it("puts a valid work") {
    forAll { sampleWork: IdentifiedBaseWork =>
      withLocalElasticsearchIndex(itemType = "work") { indexName =>
        val sampleWorkJson = toJson(sampleWork).get

        val futureIndexResponse = elasticClient.execute(
          indexInto(indexName / "work").doc(sampleWorkJson))

        whenReady(futureIndexResponse) { _ =>
          assertWorkIndexed(indexName, sampleWorkJson)
        }
      }
    }
  }

  // Possibly because the number of variations in the work model is too big,
  // a bug in the mapping related to person subjects wasn't causght by the above test.
  // So let's add a specific one
  it("puts a work with a person subject") {
    withLocalElasticsearchIndex(itemType = "work") { indexName =>
      val sampleWorkJson = toJson(createIdentifiedWorkWith(subjects = List(Subject("Daredevil", List(Unidentifiable(Person(label = "Daredevil", prefix = Some("Superhero"), numeration = Some("I")))))))).get
      val futureIndexResponse = elasticClient.execute(
        indexInto(indexName / "work").doc(sampleWorkJson))

      whenReady(futureIndexResponse) { _ =>
        assertWorkIndexed(indexName, sampleWorkJson)
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

  private def assertWorkIndexed(indexName: String, sampleWorkJson: String) = {
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
