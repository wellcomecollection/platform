package uk.ac.wellcome.test.utils

import com.sksamuel.elastic4s.http.ElasticDsl._
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.elasticsearch.mappings.WorksIndex
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

trait IndexedElasticSearchLocal
    extends ElasticSearchLocal
    with BeforeAndAfterEach { this: Suite =>

  val indexName = "records"
  val itemType = "item"

  private def createIndex(index: String) = {
    ensureIndexDeleted(index)
    new WorksIndex(elasticClient, index, itemType).create.map { _ =>
      elasticClient.execute(indexExists(index)).await.isExists should be(true)
    }.await
  }

  override def beforeEach() {
    createIndex(indexName)
  }

  def insertIntoElasticSearchWithIndex(index: String, identifiedWorks: IdentifiedWork*) = {
    if (index != indexName) {
      createIndex(index)
    }
    identifiedWorks.foreach { identifiedWork =>
      elasticClient.execute(
        indexInto(index / itemType)
          .id(identifiedWork.canonicalId)
          .doc(JsonUtil.toJson(identifiedWork).get)
      )
    }
    eventually {
      elasticClient
        .execute {
          search(index).matchAllQuery()
        }
        .await
        .hits should have size identifiedWorks.size
    }
  }

  def insertIntoElasticSearch(identifiedWorks: IdentifiedWork*): Unit =
    insertIntoElasticSearchWithIndex(indexName, identifiedWorks: _*)
}
