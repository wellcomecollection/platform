package uk.ac.wellcome.platform.archive.registrar

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl.Flow
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream

import uk.ac.wellcome.json.JsonUtil._

trait Registrar extends Logging {
  val injector: Injector

  def run() = {
    val messageStream =
      injector.getInstance(classOf[MessageStream[NotificationMessage, Object]])

    implicit val actorSystem: ActorSystem =
      injector.getInstance(classOf[ActorSystem])
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val workFlow = Flow[NotificationMessage]
      .log("notification")

    messageStream.run("registrar", workFlow)
  }
}
