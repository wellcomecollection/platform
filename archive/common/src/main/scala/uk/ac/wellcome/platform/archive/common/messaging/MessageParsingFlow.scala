package uk.ac.wellcome.platform.archive.common.messaging

import com.amazonaws.services.sqs.model.Message
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.flows.ProcessLogDiscardFlow
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage

object MessageParsingFlow extends Logging {
  def apply[T]()(implicit dec: Decoder[T]) = {
    def parse(message: Message) =
      for {
        notification <- fromJson[NotificationMessage](
          message.getBody
        )
        t <- fromJson[T](notification.Message)
      } yield t

    ProcessLogDiscardFlow[Message, T]("parse")(parse)
  }
}