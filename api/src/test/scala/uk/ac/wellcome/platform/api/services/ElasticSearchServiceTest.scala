package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.ElasticDsl._
import org.scalatest.{AsyncFunSpec, Matchers}
import uk.ac.wellcome.models.{
  IdentifiedUnifiedItem,
  SourceIdentifier,
  UnifiedItem
}
import uk.ac.wellcome.platform.api.models.Record
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.JsonUtil

class ElasticSearchServiceTest
    extends AsyncFunSpec
    with ElasticSearchLocal
    with Matchers {

  val itemType = "item"
  val elasticService = new ElasticSearchService(index, itemType, elasticClient)

  it("should return the records in Elasticsearch") {
    val firstIdentifiedUnifiedItem =
      identifiedUnifiedItemWith(canonicalId = "1234",
                                label = "this is the first item label")
    val secondIdentifiedUnifiedItem =
      identifiedUnifiedItemWith(canonicalId = "5678",
                                label = "this is the second item label")
    insertIntoElasticSearch(firstIdentifiedUnifiedItem,
                            secondIdentifiedUnifiedItem)

    val recordsFuture = elasticService.findRecords()

    recordsFuture map { records =>
      records should have size 2
      records.head shouldBe Record(
        "Work",
        firstIdentifiedUnifiedItem.canonicalId,
        firstIdentifiedUnifiedItem.unifiedItem.label)
      records.tail.head shouldBe Record(
        "Work",
        secondIdentifiedUnifiedItem.canonicalId,
        secondIdentifiedUnifiedItem.unifiedItem.label)
    }
  }

  it("should find a record by id") {
    insertIntoElasticSearch(
      identifiedUnifiedItemWith(canonicalId = "1234",
                                label = "this is the item label"))

    val recordsFuture = elasticService.findRecordById("1234")

    recordsFuture map { records =>
      records.isDefined shouldBe true
      records.get shouldBe Record("Work", "1234", "this is the item label")
    }
  }

  private def insertIntoElasticSearch(
    identifiedUnifiedItems: IdentifiedUnifiedItem*) = {
    identifiedUnifiedItems.foreach { identifiedUnifiedItem =>
      elasticClient.execute(
        indexInto(index / itemType)
          .doc(JsonUtil.toJson(identifiedUnifiedItem).get))
    }
    eventually {
      elasticClient
        .execute {
          search(index).matchAll()
        }
        .await
        .hits should have size identifiedUnifiedItems.size
    }
  }

  private def identifiedUnifiedItemWith(canonicalId: String, label: String) = {
    IdentifiedUnifiedItem(canonicalId,
                          UnifiedItem(identifiers = List(
                                        SourceIdentifier(source = "Calm",
                                                         sourceId = "AltRefNo",
                                                         value = "calmid")),
                                      label = label))
  }
}
