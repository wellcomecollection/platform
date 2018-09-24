package uk.ac.wellcome.platform.archive.common.flows

import io.circe.Decoder
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage


object NotificationParsingFlow {
  def apply[T]()(implicit dec: Decoder[T]) = {
    def parse(msg: NotificationMessage) =
      fromJson[T](msg.Message)

    ProcessLogDiscardFlow[NotificationMessage, T]("parse_notification")(parse)
  }
}
