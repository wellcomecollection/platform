package uk.ac.wellcome.platform.archive.registrar.http.models
import io.circe.generic.extras.JsonKey

case class DisplayBagInfo(
  externalIdentifier: String,
  @JsonKey("type")
  ontologyType: String = "BagInfo"
                         )
