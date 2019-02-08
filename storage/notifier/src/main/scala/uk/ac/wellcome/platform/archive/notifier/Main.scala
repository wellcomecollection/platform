package uk.ac.wellcome.platform.archive.notifier

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.sns.model.PublishResult
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.SNSBuilder
import uk.ac.wellcome.platform.archive.common.config.builders._
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val materializer: ActorMaterializer =
      AkkaBuilder.buildActorMaterializer()

    val messageStream =
      MessagingBuilder.buildMessageStream[NotificationMessage, PublishResult](
        config)

    new Notifier(
      messageStream = messageStream,
      snsClient = SNSBuilder.buildSNSClient(config),
      snsConfig = SNSBuilder.buildSNSConfig(config),
      contextUrl = HTTPServerBuilder.buildContextURL(config)
    )
  }
}
