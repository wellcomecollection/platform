package uk.ac.wellcome.messaging.message

import io.circe.Encoder
import uk.ac.wellcome.messaging.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.storage.s3.S3TypeStore
import uk.ac.wellcome.utils.JsonUtil.toJson

import scala.concurrent.{ExecutionContext, Future}

trait MessageSender[T] {
  def send(message: T, subject: String)(
    implicit encoder: Encoder[T], ec: ExecutionContext
  ): Future[PublishAttempt]
}

class S3TypeMessageSender[T](sns: SNSWriter, s3TypeStore: S3TypeStore[T])(
  implicit encoderM: Encoder[MessagePointer]
) extends MessageSender[T] {
  def send(message: T, subject: String)(
    implicit encoderT: Encoder[T], ec: ExecutionContext
  ): Future[PublishAttempt] = for {
    location <- s3TypeStore.put(message, subject)
    pointer <- Future.fromTry(toJson(MessagePointer(location)))
    publishAttempt <- sns.writeMessage(pointer, subject)
  } yield publishAttempt
}

class TypeMessageSender[T](sns: SNSWriter) extends MessageSender[T] {
  def send(message: T, subject: String)(
    implicit encoder: Encoder[T], ec: ExecutionContext
  ): Future[PublishAttempt] = for {
    jsonMessage <- Future.fromTry(toJson(message))
    publishAttempt <- sns.writeMessage(jsonMessage, subject)
  } yield publishAttempt
}