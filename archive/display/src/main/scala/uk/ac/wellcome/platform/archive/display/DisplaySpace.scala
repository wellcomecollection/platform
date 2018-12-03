package uk.ac.wellcome.platform.archive.display

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.models.Namespace

case class DisplaySpace(
  id: String,
  @JsonKey("type") ontologyType: String = "Space"
)

case object DisplaySpace {
  def apply(namespace: Namespace): DisplaySpace =
    DisplaySpace(id = namespace.underlying)
}
