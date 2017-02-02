package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticDsl._
import scala.concurrent.ExecutionContext.Implicits.global

import uk.ac.wellcome.platform.api.models._


@Singleton
class CalmService @Inject()(
  elasticsearchService: ElasticsearchService
) {
  def findByAltRefNo(altRefNo: String) = elasticsearchService.client.execute {
    search in "records" -> "item" query {
      bool {
        must(
          matchQuery("AltRefNo.keyword", altRefNo)
        )
      }
    }
  }.map { result =>
    if (result.isEmpty) {
      Nil
    } else {
      result.hits.map { Record(_) }
    }
  }
}
