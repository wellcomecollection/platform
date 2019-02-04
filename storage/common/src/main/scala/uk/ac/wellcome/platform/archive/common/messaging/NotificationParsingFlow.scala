package uk.ac.wellcome.platform.archive.common.messaging

import akka.NotUsed
import akka.stream.scaladsl.Flow
import io.circe.Decoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archive.common.flows.ProcessLogDiscardFlow

/** Parses a NotificationMessage as an instance of type T, and only
  * emits the successfully parsed results.
  *
  */
object NotificationParsingFlow {
  def apply[T]()(
    implicit dec: Decoder[T]): Flow[NotificationMessage, T, NotUsed] =
    ProcessLogDiscardFlow[NotificationMessage, T]("parse_notification") {
      notificationMessage: NotificationMessage =>
        fromJson[T](notificationMessage.body)
    }
}
