package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.ElasticDsl.indexInto
import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.scalatest.{AsyncFunSpec, Matchers}
import uk.ac.wellcome.finatra.services.ElasticsearchService
import uk.ac.wellcome.models.{Identifier, UnifiedItem}
import uk.ac.wellcome.platform.api.models.Record

class CalmServiceTest extends AsyncFunSpec with ElasticSugar with Matchers{

  val elasticSearchService = new ElasticsearchService(client)
  val calmService = new CalmService(elasticSearchService)

  it("should find records") {
    ensureIndexExists("records")
    insertIntoElasticSearch(UnifiedItem("id", List(Identifier("Calm", "AltRefNo", "calmid")), None))

    val recordsFuture = calmService.findRecords()

    recordsFuture map {records=>
      records should have size 1
      records.head shouldBe Record("Work", "id")
    }
  }

  private def insertIntoElasticSearch(unifiedItem: UnifiedItem) = {
    client.execute(indexInto("records" / "item").doc(UnifiedItem.json(unifiedItem)))
    blockUntilCount(1,"records")
  }
}
