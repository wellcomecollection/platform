package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent.Future

import uk.ac.wellcome.finatra.services.ElasticsearchService
import uk.ac.wellcome.platform.api.models._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

@Singleton
class CalmService @Inject()(
  elasticsearchService: ElasticsearchService
) {

  private def parentAltRefNo(altRefNo: String): String =
    altRefNo.take(altRefNo.lastIndexOf("/"))

  def findParentCollectionByAltRefNo(
    altRefNo: String): Future[Option[Collection]] =
    findCollectionByAltRefNo(parentAltRefNo(altRefNo))

  def findRecordByAltRefNo(altRefNo: String): Future[Option[Record]] =
    elasticsearchService.client
      .execute {
        search("records/item").query(
          boolQuery().must(matchQuery("AltRefNo.keyword", altRefNo))
        )
      }
      .map { _.hits.headOption.map { Record(_) } }

  def findRecords(): Future[Array[Record]] =
    elasticsearchService.client
      .execute {
        search("records/item").matchAll().limit(10)
      }
      .map { _.hits.map { Record(_) } }

  def findCollectionByAltRefNo(altRefNo: String): Future[Option[Collection]] = {
    elasticsearchService.client
      .execute {
        search("records/collection").query(
          boolQuery().must(matchQuery("AltRefNo.keyword", altRefNo))
        )
      }
      .map { _.hits.headOption.map { Collection(altRefNo, _) } }
  }
}
