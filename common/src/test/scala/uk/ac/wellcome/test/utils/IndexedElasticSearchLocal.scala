package uk.ac.wellcome.test.utils

import com.sksamuel.elastic4s.http.ElasticDsl.{
  indexExists,
  indexInto,
  search,
  _
}
import com.sksamuel.elastic4s.http.index.IndexResponse
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.elasticsearch.WorksIndex
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

trait IndexedElasticSearchLocal
    extends ElasticSearchLocal
    with BeforeAndAfterEach { this: Suite =>

  val indexName: String
  val itemType: String

  private def createIndex(index: String) = {
    ensureIndexDeleted(index)
    new WorksIndex(elasticClient, index, itemType).create.map { _ =>
      elasticClient.execute(indexExists(index)).await.isExists should be(true)
    }.await
  }

  override def beforeEach() {
    createIndex(indexName)
  }

  def insertIntoElasticSearchWithIndex(index: String, works: Work*) = {
    if (index != indexName) {
      createIndex(index)
    }

    works.foreach { work =>
      val jsonDoc = JsonUtil.toJson(work).get

      val result: Future[IndexResponse] = elasticClient.execute(
        indexInto(index / itemType)
          .id(work.id)
          .doc(jsonDoc)
      )

      result.map { indexResponse =>
        print(indexResponse)
      }
    }
    eventually {
      elasticClient
        .execute {
          search(index).matchAllQuery()
        }
        .await
        .hits should have size works.size
    }
  }

  def insertIntoElasticSearch(works: Work*): Unit =
    insertIntoElasticSearchWithIndex(indexName, works: _*)
}
