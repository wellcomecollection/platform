package uk.ac.wellcome.platform.ingestor.services

import com.fasterxml.jackson.core.JsonParseException
import com.sksamuel.elastic4s.ElasticDsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

class IdentifiedWorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticSearchLocal {

  val identifiedWorkIndexer =
    new IdentifiedWorkIndexer(index, itemType, elasticClient)

  def identifiedWorkJson(canonicalId: String, sourceId: String, label: String): String = {
    JsonUtil.toJson(
        IdentifiedWork(
          canonicalId = canonicalId,
          work = Work(
            identifiers = List(SourceIdentifier("Miro", "MiroID", sourceId)), label = label)))
      .get
  }

  it("should insert an identified unified item into Elasticsearch") {

    val identifiedWorkString =
      identifiedWorkJson("5678", "1234", "some label")

    val future = identifiedWorkIndexer.indexIdentifiedWork(
      identifiedWorkString)

    whenReady(future) { _ =>
      eventually {
        val hits = elasticClient
          .execute(search(s"$index/$itemType").matchAll().limit(100))
          .map { _.hits }
          .await
        hits should have size 1
        hits.head.sourceAsString shouldBe identifiedWorkString
      }
    }

  }

  it("should add only one record when multiple records with same id are ingested") {
    val identifiedWorkString =
      identifiedWorkJson("5678", "1234", "some label")

    val future = Future.sequence(
      (1 to 2).map(_ => identifiedWorkIndexer.indexIdentifiedWork(
        identifiedWorkString))
    )

    whenReady(future) { _ =>
      eventually {
        val hits = elasticClient
          .execute(search(s"$index/$itemType").matchAll().limit(100))
          .map { _.hits }
          .await
        hits should have size 1
        hits.head.sourceAsString shouldBe identifiedWorkString
      }
    }

  }

  it("should return a failed future if the input string is not an identified work") {
    val future = identifiedWorkIndexer.indexIdentifiedWork("a document")

    whenReady(future.failed) { exception =>
      exception shouldBe a[JsonParseException]
    }
  }
}
