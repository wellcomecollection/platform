package uk.ac.wellcome.test.utils

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.IndexResponse
import org.elasticsearch.index.VersionType
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.elasticsearch.WorksIndex
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

trait IndexedElasticSearchLocal
    extends ElasticSearchLocal
    with BeforeAndAfterEach { this: Suite =>

  val indexName: String
  val itemType: String

  def esLocalFlags = Map(
    "es.host" -> "localhost",
    "es.port" -> "9200",
    "es.name" -> "wellcome",
    "es.index" -> indexName,
    "es.type" -> itemType
  )

  private def createIndex(index: String) = {
    ensureIndexDeleted(index)
    new WorksIndex(elasticClient, index, itemType).create.map { _ =>
      elasticClient.execute(indexExists(index)).await.isExists should be(true)
    }.await
  }

  override def beforeEach() {
    createIndex(indexName)
  }

  def insertIntoElasticSearchWithIndex(index: String, works: IdentifiedWork*) = {
    if (index != indexName) {
      createIndex(index)
    }

    works.foreach { work =>
      val jsonDoc = toJson(work).get

      val result: Future[IndexResponse] = elasticClient.execute(
        indexInto(index / itemType)
          .version(work.version)
          .versionType(VersionType.EXTERNAL_GTE)
          .id(work.canonicalId)
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

  def insertIntoElasticSearch(works: IdentifiedWork*): Unit =
    insertIntoElasticSearchWithIndex(indexName, works: _*)
}
