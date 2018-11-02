package uk.ac.wellcome.platform.archive.common.models

import io.circe.generic.extras.JsonKey

case class NotificationMessage(
  @JsonKey("Message") body: String
)
