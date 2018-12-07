package uk.ac.wellcome.messaging.sns

import grizzled.slf4j.Logging
import io.circe.Encoder

import scala.concurrent.Future

/** Writes messages to SNS.  This class is configured with a single topic in
  * `snsConfig`, and writes to the same topic on every request.
  *
  */
class SNSWriter(snsMessageWriter: SNSMessageWriter,
                snsConfig: SNSConfig)
    extends Logging {

  def writeMessage(message: String, subject: String): Future[PublishAttempt] =
    snsMessageWriter.writeMessage(
      message = message,
      subject = subject,
      snsConfig = snsConfig
    )

  def writeMessage[T](message: T, subject: String)(
    implicit encoder: Encoder[T]): Future[PublishAttempt] =
    snsMessageWriter.writeMessage[T](
      message = message,
      subject = subject,
      snsConfig = snsConfig
    )
}
