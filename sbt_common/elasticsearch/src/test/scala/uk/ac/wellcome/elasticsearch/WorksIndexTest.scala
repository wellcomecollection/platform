package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{indexInto, search, _}
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{ElasticError, Response}
import io.circe.Encoder
import org.scalacheck.Shrink
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import org.scalacheck.ScalacheckShapeless._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{
  IdentifiedBaseWork,
  Person,
  Subject,
  Unidentifiable
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorksIndexTest
    extends FunSpec
    with ElasticsearchFixtures
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonAssertions
    with PropertyChecks with WorksGenerators {

  // On failure, scalacheck tries to shrink to the smallest input that causes a failure.
  // With IdentifiedWork, that means that it never actually completes.
  implicit val noShrink = Shrink.shrinkAny[IdentifiedBaseWork]

  it("puts a valid work") {
    forAll { sampleWork: IdentifiedBaseWork =>
      withLocalWorksIndex { indexName: String =>
        whenReady(indexObject(indexName, sampleWork)) { _ =>
          assertObjectIndexed(indexName, sampleWork)
        }
      }
    }
  }

  // Possibly because the number of variations in the work model is too big,
  // a bug in the mapping related to person subjects wasn't caught by the above test.
  // So let's add a specific one
  it("puts a work with a person subject") {
    withLocalWorksIndex { indexName =>
      val sampleWork = createIdentifiedWorkWith(
        subjects = List(
          Subject(
            label = "Daredevil",
            concepts = List(
              Unidentifiable(Person(
                label = "Daredevil",
                prefix = Some("Superhero"),
                numeration = Some("I")
              ))
            )
          )
        )
      )
      whenReady(indexObject(indexName, sampleWork)) { _ =>
        assertObjectIndexed(indexName, sampleWork)
      }
    }
  }

  it("does not put an invalid work") {
    withLocalWorksIndex { indexName =>
      val badTestObject = BadTestObject(
        id = "id",
        weight = 5
      )

      whenReady(indexObject(indexName, badTestObject)) { response =>
        response.isError shouldBe true
        response.error shouldBe a[ElasticError]
      }
    }
  }

  private def indexObject[T](indexName: String, t: T)(implicit encoder: Encoder[T]): Future[Response[IndexResponse]] =
    elasticClient
      .execute {
        indexInto(indexName / indexName).doc(toJson(t).get)
      }

  private def assertObjectIndexed[T](indexName: String, t: T)(implicit encoder: Encoder[T]): Assertion =
    // Elasticsearch is eventually consistent so, when the future completes,
    // the documents might not immediately appear in search
    eventually {
      val response: Response[SearchResponse] = elasticClient
        .execute {
          search(indexName).matchAllQuery()
        }
        .await

      val hits = response.result.hits.hits

      hits should have size 1
      assertJsonStringsAreEqual(hits.head.sourceAsString, toJson(t).get)
    }
}
