package uk.ac.wellcome.platform.ingestor.services

import com.fasterxml.jackson.core.JsonParseException
import com.sksamuel.elastic4s.ElasticDsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

class IdentifiedUnifiedItemIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticSearchLocal {

  val itemType = "item"

  val identifiedUnifiedItemIndexer =
    new IdentifiedUnifiedItemIndexer(index, itemType, elasticClient)

  def identifiedUnifiedItemJson(canonicalId: String, sourceId: String, label: String): String = {
    JsonUtil.toJson(
        IdentifiedUnifiedItem(
          canonicalId = canonicalId,
          unifiedItem = UnifiedItem(
            identifiers = List(SourceIdentifier("Miro", "MiroID", sourceId)), label = label)))
      .get
  }

  it("should insert an identified unified item into Elasticsearch") {

    val identifiedUnifiedItemString =
      identifiedUnifiedItemJson("5678", "1234", "some label")

    val future = identifiedUnifiedItemIndexer.indexIdentifiedUnifiedItem(
      identifiedUnifiedItemString)

    whenReady(future) { _ =>
      eventually {
        val hits = elasticClient
          .execute(search(s"$index/$itemType").matchAll().limit(100))
          .map { _.hits }
          .await
        hits should have size 1
        hits.head.sourceAsString shouldBe identifiedUnifiedItemString
      }
    }

  }

  it("should add only one record when multiple records with same id are ingested") {
    val identifiedUnifiedItemString =
      identifiedUnifiedItemJson("5678", "1234", "some label")

    val future = Future.sequence(
      (1 to 2).map(_ => identifiedUnifiedItemIndexer.indexIdentifiedUnifiedItem(
        identifiedUnifiedItemString))
    )

    whenReady(future) { _ =>
      eventually {
        val hits = elasticClient
          .execute(search(s"$index/$itemType").matchAll().limit(100))
          .map { _.hits }
          .await
        hits should have size 1
        hits.head.sourceAsString shouldBe identifiedUnifiedItemString
      }
    }

  }

  it("should return a failed future if the input string is not an identified unified item") {
    val future = identifiedUnifiedItemIndexer.indexIdentifiedUnifiedItem("a document")

    whenReady(future.failed) { exception =>
      exception shouldBe a[JsonParseException]
    }
  }
}
