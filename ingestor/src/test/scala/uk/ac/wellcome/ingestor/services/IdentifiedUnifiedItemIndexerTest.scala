package uk.ac.wellcome.ingestor.services

import com.fasterxml.jackson.core.JsonParseException
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.search
import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.ingestor.services.IdentifiedUnifiedItemIndexer
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

class IdentifiedUnifiedItemIndexerTest extends FunSpec with ScalaFutures with Matchers with ElasticSugar{


  ensureIndexExists("records")

  it("should insert an identified unified item into elastic search"){
    val identifiedUnifiedItemIndexer = new IdentifiedUnifiedItemIndexer("records", "item", client)
    val identifiedUnifiedItemString = JsonUtil.toJson(IdentifiedUnifiedItem(canonicalId = "5678",unifiedItem =UnifiedItem(identifiers =
      List(SourceIdentifier("Miro", "MiroID", "1234"))))).get

    val future = identifiedUnifiedItemIndexer.indexUnifiedItem(identifiedUnifiedItemString)

    whenReady(future){_ =>
      blockUntilCount(1, "records" / "item")
      val hits = client.execute(search("records/item").matchAll().limit(100)).map{_.hits}.await
      hits should have size 1
      hits.head.sourceAsString shouldBe identifiedUnifiedItemString
    }

  }

  it("should return a failed future if the input string is not an identified unified item"){
    val identifiedUnifiedItemIndexer = new IdentifiedUnifiedItemIndexer("records", "item", client)
    val future = identifiedUnifiedItemIndexer.indexUnifiedItem("a document")

    whenReady(future.failed){exception =>
      exception shouldBe a [JsonParseException]
    }
  }
}
