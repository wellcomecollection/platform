package uk.ac.wellcome.platform.archive.display

import io.circe.generic.extras.JsonKey

case class DisplayStorageSpace(
  id: String,
  @JsonKey("type") ontologyType: String = "Space"
)
