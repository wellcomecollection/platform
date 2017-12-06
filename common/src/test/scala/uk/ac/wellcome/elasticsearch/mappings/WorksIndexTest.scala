package uk.ac.wellcome.elasticsearch.mappings

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import org.elasticsearch.client.ResponseException
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifierSchemes, _}
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

class WorksIndexTest
    extends FunSpec
    with ElasticSearchLocal
    with ScalaFutures
    with Eventually
    with Matchers
    with BeforeAndAfterEach {

  val indexName = "records"
  val itemType = "item"

  val worksIndex = new WorksIndex(elasticClient, indexName, itemType)

  override def beforeEach(): Unit = {
    ensureIndexDeleted(indexName)
  }

  it(
    "should create an index where it's possible to insert and retrieve a valid Work json") {
    createAndWaitIndexIsCreated()

    val identifiers = List(
      SourceIdentifier(identifierScheme = IdentifierSchemes.miroImageNumber,
                       value = "4321"))
    val workJson = JsonUtil
      .toJson(
        Work(
          canonicalId = Some("1234"),
          identifiers = identifiers,
          title = "A magical menagerie for magpies",
          items = List(
            Item(
              canonicalId = Some("56789"),
              identifiers = identifiers,
              locations = List(
                Location(locationType = "iiif",
                         url =
                           Some("https://iiif.wellcomecollection.org/image"),
                         license = License_CCBY))
            ))
        )
      )
      .get

    elasticClient.execute(indexInto(indexName / itemType).doc(workJson))

    eventually {
      val hits = elasticClient
        .execute(search(s"$indexName/$itemType").matchAllQuery())
        .map {
          _.hits.hits
        }
        .await
      hits should have size 1
      shouldBeSameJson(hits.head.sourceAsString, workJson)
    }
  }

  it(
    "it should create an index where inserting a document that does not match the mapping of a work fails") {
    createAndWaitIndexIsCreated()

    val eventualIndexResponse = elasticClient.execute(
      indexInto(indexName / itemType)
        .doc("""{"json":"json not matching the index structure"}"""))

    whenReady(eventualIndexResponse.failed) { exception =>
      exception shouldBe a[ResponseException]
    // TODO: this should be more specific
    }
  }

  it("should update an already existing index with the mapping") {
    createIndexAndInsertDocument()

    worksIndex.create

    // Check that the mapping wasn't found.  We get a 404 error from
    // Elasticsearch if we try to look up a non-existent mapping, which
    // causes the `eventually` to fail.
    eventually {
      elasticClient
        .execute(getMapping(indexName))
        .await
    }
  }

  private def shouldBeSameJson(actualJson: String, workJson: String): Any = {
    JsonUtil.fromJson[Work](actualJson) shouldBe JsonUtil.fromJson[Work](
      workJson)
  }

  private def createIndexAndInsertDocument() = {
    val futureIndexWithDocument =
      for {
        _ <- elasticClient.execute(createIndex(indexName))
        _ <- elasticClient.execute(
          putMapping(indexName / itemType)
            .dynamic(DynamicMapping.Strict)
            .as(keywordField("canonicalId")))
        _ <- elasticClient.execute(
          indexInto(indexName / itemType).doc("""
            |{
            | "canonicalId": "1234"
            |}
          """.stripMargin))
      } yield ()

    futureIndexWithDocument.await
  }

  private def createAndWaitIndexIsCreated() = {
    val createIndexFuture = worksIndex.create

    whenReady(createIndexFuture) { _ =>
      eventually {
        elasticClient.execute(indexExists(indexName)).await.isExists should be(
          true)
      }
    }
  }
}
