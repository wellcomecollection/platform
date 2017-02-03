package uk.ac.wellcome.platform.api.models

import com.sksamuel.elastic4s.searches.RichSearchHit


case class Collection(
  refno: String,
  title: String
)
case object Collection {
  def apply(altRefNo: String, esResult: RichSearchHit): Collection = {
    val data = esResult.sourceAsMap

    Collection(altRefNo, data("Title").toString)
  }
}
