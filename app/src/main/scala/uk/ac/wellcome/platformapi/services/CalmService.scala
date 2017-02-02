package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import uk.ac.wellcome.platform.api.models._


@Singleton
class CalmService @Inject()(
  elasticsearchService: ElasticsearchService
) {

  def findRecordsByAltRefNo(altRefNo: String): Future[Array[Record]] =
    elasticsearchService.client.execute {
      search("records/item").query(
        boolQuery().must(matchQuery("AltRefNo.keyword", altRefNo))
      )
    }.map { _.hits.map { Record(_) }}

}
