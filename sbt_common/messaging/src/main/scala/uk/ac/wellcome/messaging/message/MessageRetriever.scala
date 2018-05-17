package uk.ac.wellcome.messaging.message

import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.storage.s3.S3TypeStore
import uk.ac.wellcome.utils.JsonUtil.fromJson

import scala.concurrent.{ExecutionContext, Future}

trait MessageRetriever[T] {
  def retrieve(notification: NotificationMessage)(
    implicit decoderT: Decoder[T], ec: ExecutionContext
  ): Future[T]
}

class S3TypeMessageRetriever[T] @Inject()(s3TypeStore: S3TypeStore[T])(
  implicit decoderP: Decoder[MessagePointer]
) extends MessageRetriever[T] {
  def retrieve(notification: NotificationMessage)(
    implicit decoderT: Decoder[T], ec: ExecutionContext
  ): Future[T] = for {
    messagePointer <- Future.fromTry(
      fromJson[MessagePointer](notification.Message))
    deserialisedObject <- s3TypeStore.get(messagePointer.src)
  } yield deserialisedObject
}

class TypeMessageRetriever[T] @Inject()()
  extends MessageRetriever[T] {
  def retrieve(notification: NotificationMessage)(
    implicit decoderT: Decoder[T], ec: ExecutionContext
  ): Future[T] = for {
    deserialisedObject <- Future.fromTry(
      fromJson[T](notification.Message)
    )
  } yield deserialisedObject
}