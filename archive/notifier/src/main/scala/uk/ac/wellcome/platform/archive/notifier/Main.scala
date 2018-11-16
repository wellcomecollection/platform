package uk.ac.wellcome.platform.archive.notifier

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.sns.model.PublishResult
import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SNSBuilder
import uk.ac.wellcome.platform.archive.common.config.builders._
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val materializer: ActorMaterializer =
    AkkaBuilder.buildActorMaterializer()

  val messageStream =
    MessagingBuilder.buildMessageStream[NotificationMessage, PublishResult](
      config)

  val notifier = new Notifier(
    messageStream = messageStream,
    snsClient = SNSBuilder.buildSNSClient(config),
    snsConfig = SNSBuilder.buildSNSConfig(config),
    contextUrl = HTTPServerBuilder.buildContextURL(config)
  )

  run(notifier)
}
