package uk.ac.wellcome.platform.archiver.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.json.JsonUtil._

import scala.util.{Failure, Success}

object DownloadNotificationFlow extends Logging {
  def apply(): Flow[NotificationMessage, ObjectLocation, NotUsed] = {
    Flow[NotificationMessage]
      .log("notification message")
      .map((m: NotificationMessage) => fromJson[ObjectLocation](m.Message))
      .map {
        case Success(objectLocation) => objectLocation
        case Failure(e) => throw e
      }
      .log("download location")
  }
}
