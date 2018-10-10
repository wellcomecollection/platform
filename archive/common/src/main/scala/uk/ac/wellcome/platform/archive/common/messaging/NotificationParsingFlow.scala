package uk.ac.wellcome.platform.archive.common.messaging

import io.circe.Decoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.flows.ProcessLogDiscardFlow
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage

/** Parses a [[NotificationMessage]] as an instance of type T, and only
  * emits the successfully parsed results.
  *
  */
object NotificationParsingFlow {
  def apply[T]()(implicit dec: Decoder[T]) = {
    def parse(msg: NotificationMessage) =
      fromJson[T](msg.Message)

    ProcessLogDiscardFlow[NotificationMessage, T]("parse_notification")(parse)
  }
}
