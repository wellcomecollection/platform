package uk.ac.wellcome.platform.archive.display

import io.circe.generic.extras.JsonKey

case class DisplaySpace(
  id: String,
  @JsonKey("type") ontologyType: String = "Space"
)
