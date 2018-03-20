package uk.ac.wellcome.elasticsearch.test.utils

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.IndexResponse
import org.elasticsearch.index.VersionType
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.elasticsearch.WorksIndex
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.utils.JsonTestUtil

import scala.concurrent.Future

trait IndexedElasticSearchLocal
    extends ElasticSearchLocal
    with BeforeAndAfterEach
    with JsonTestUtil { this: Suite =>

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

  def assertElasticsearchEventuallyHasWork(work: IdentifiedWork) = {
    val workJson = toJson(work).get

    eventually {
      val hits = elasticClient
        .execute(search(s"$indexName/$itemType").matchAllQuery().limit(100))
        .map { _.hits.hits }
        .await

      hits should have size 1

      assertJsonStringsAreEqual(hits.head.sourceAsString, workJson)
    }
  }
}
