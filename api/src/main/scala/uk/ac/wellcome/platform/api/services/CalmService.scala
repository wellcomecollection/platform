package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import uk.ac.wellcome.platform.api.models._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

@Singleton
class CalmService @Inject()(elasticClient: ElasticClient
) {

  def findRecordByAltRefNo(altRefNo: String): Future[Option[Record]] =
    elasticClient
      .execute {
        search("records/item").query(
          boolQuery().must(matchQuery("AltRefNo.keyword", altRefNo))
        )
      }
      .map { _.hits.headOption.map { Record(_) } }

  def findRecords(): Future[Array[Record]] =
    elasticClient
      .execute {
        search("records/item").matchAll().limit(10)
      }
      .map { _.hits.map { Record(_) } }

}
