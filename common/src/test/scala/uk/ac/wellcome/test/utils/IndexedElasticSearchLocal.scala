package uk.ac.wellcome.test.utils

import com.sksamuel.elastic4s.ElasticDsl._
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

  override def beforeEach(): Unit = {
    ensureIndexDeleted(indexName)
    new WorksIndex(elasticClient, indexName, itemType).create.map { _ =>
      elasticClient.execute(indexExists(indexName)).await.isExists should be(true)
    }.await
  }

  def insertIntoElasticSearch(identifiedWorks: IdentifiedWork*): Unit = {
    identifiedWorks.foreach { identifiedWork =>
      elasticClient.execute(
        indexInto(indexName / itemType)
          .id(identifiedWork.canonicalId)
          .doc(JsonUtil.toJson(identifiedWork).get)
      )
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
}
