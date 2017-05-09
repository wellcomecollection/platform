package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.ElasticDsl._
import org.scalatest.{AsyncFunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.platform.api.models.Record
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal
import uk.ac.wellcome.utils.JsonUtil

class ElasticSearchServiceTest
    extends AsyncFunSpec
    with IndexedElasticSearchLocal
    with Matchers {

  val elasticService =
    new ElasticSearchService(indexName, itemType, elasticClient)

  it("should return the records in Elasticsearch") {
    val firstIdentifiedWork =
      identifiedWorkWith(canonicalId = "1234",
                         label = "this is the first item label")
    val secondIdentifiedWork =
      identifiedWorkWith(canonicalId = "5678",
                         label = "this is the second item label")
    insertIntoElasticSearch(firstIdentifiedWork, secondIdentifiedWork)

    val recordsFuture = elasticService.findRecords()

    recordsFuture map { records =>
      records should have size 2
      records.head shouldBe Record("Work",
                                   firstIdentifiedWork.canonicalId,
                                   firstIdentifiedWork.work.label)
      records.tail.head shouldBe Record("Work",
                                        secondIdentifiedWork.canonicalId,
                                        secondIdentifiedWork.work.label)
    }
  }

  it("should find a record by id") {
    insertIntoElasticSearch(
      identifiedWorkWith(canonicalId = "1234",
                         label = "this is the item label"))

    val recordsFuture = elasticService.findRecordById("1234")

    recordsFuture map { records =>
      records.isDefined shouldBe true
      records.get shouldBe Record("Work", "1234", "this is the item label")
    }
  }

  private def insertIntoElasticSearch(identifiedWorks: IdentifiedWork*) = {
    identifiedWorks.foreach { identifiedWork =>
      elasticClient.execute(
        indexInto(indexName / itemType)
          .doc(JsonUtil.toJson(identifiedWork).get))
    }
    eventually {
      elasticClient
        .execute {
          search(indexName).matchAllQuery()
        }
        .await
        .hits should have size identifiedWorks.size
    }
  }

  private def identifiedWorkWith(canonicalId: String, label: String) = {
    IdentifiedWork(canonicalId,
                   Work(identifiers = List(
                          SourceIdentifier(source = "Calm",
                                           sourceId = "AltRefNo",
                                           value = "calmid")),
                        label = label))
  }
}
