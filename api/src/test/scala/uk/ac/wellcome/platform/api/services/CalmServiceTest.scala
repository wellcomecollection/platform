package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.scalatest.{AsyncFunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.api.models.Record
import uk.ac.wellcome.utils.JsonUtil

class CalmServiceTest extends AsyncFunSpec with ElasticSugar with Matchers {

  val calmService = new CalmService(client)

  it("should find records") {
    ensureIndexExists("records")
    insertIntoElasticSearch(
      IdentifiedUnifiedItem("id",
                            UnifiedItem(
                              identifiers = List(
                                SourceIdentifier(source = "Calm",
                                                 sourceId = "AltRefNo",
                                                 value = "calmid")), label = "this is the item label")))

    val recordsFuture = calmService.findRecords()

    recordsFuture map { records =>
      records should have size 1
      records.head shouldBe Record("Work", "id", "this is the item label")
    }
  }

  private def insertIntoElasticSearch(
    identifiedUnifiedItem: IdentifiedUnifiedItem) = {
    client.execute(
      indexInto("records" / "item")
        .doc(JsonUtil.toJson(identifiedUnifiedItem).get))
    blockUntilCount(1, "records")
  }
}
