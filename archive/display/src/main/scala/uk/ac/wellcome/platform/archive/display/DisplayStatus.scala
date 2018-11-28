package uk.ac.wellcome.platform.archive.display

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.progress.models.{Callback, Progress}

case class DisplayStatus(
  id: String,
  @JsonKey("type") ontologyType: String = "Status"
)

case object DisplayStatus {
  def apply(progressStatus: Progress.Status): DisplayStatus =
    DisplayStatus(progressStatus.toString)

  def apply(callbackStatus: Callback.CallbackStatus): DisplayStatus =
    DisplayStatus(callbackStatus.toString)
}
