package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.scalatest.{AsyncFunSpec, Matchers}
import uk.ac.wellcome.finatra.services.ElasticsearchService
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.api.models.Record
import uk.ac.wellcome.utils.JsonUtil

class CalmServiceTest extends AsyncFunSpec with ElasticSugar with Matchers {

  val elasticSearchService = new ElasticsearchService(client)
  val calmService = new CalmService(elasticSearchService)

  it("should find records") {
    ensureIndexExists("records")
    insertIntoElasticSearch(
      IdentifiedUnifiedItem(
        "id",
        UnifiedItem(List(SourceIdentifier("Calm", "AltRefNo", "calmid")),
                    None)))

    val recordsFuture = calmService.findRecords()

    recordsFuture map { records =>
      records should have size 1
      records.head shouldBe Record("Work", "id")
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
