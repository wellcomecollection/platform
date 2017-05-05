package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.searches.RichSearchHit
import uk.ac.wellcome.models.{IdentifiedWork, Work}
import uk.ac.wellcome.utils.JsonUtil

case class Record(
  @JsonProperty("type") ontologyType: String = "Work",
  id: String,
  label: String
)
case object Record {
  def apply(hit: RichSearchHit): Record = {
    val identifiedWork =
      JsonUtil.fromJson[IdentifiedWork](hit.sourceAsString).get

    Record(
      id = identifiedWork.canonicalId,
      label = identifiedWork.work.label
    )
  }
}
