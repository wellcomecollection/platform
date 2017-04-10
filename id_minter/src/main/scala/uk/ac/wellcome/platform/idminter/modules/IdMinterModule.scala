package uk.ac.wellcome.platform.idminter.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, UnifiedItem}
import uk.ac.wellcome.platform.idminter.steps.{
  IdentifierGenerator,
  UnifiedItemExtractor
}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.utils.{JsonUtil, TryBackoff}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

object IdMinterModule extends TwitterModule with TryBackoff {
  val snsSubject = "identified-item"

  override def singletonStartup(injector: Injector) {
    info("Starting IdMinter module")

    val sqsReader = injector.instance[SQSReader]
    val idGenerator = injector.instance[IdentifierGenerator]
    val snsWriter = injector.instance[SNSWriter]
    val actorSystem = injector.instance[ActorSystem]
    run(() => start(sqsReader, idGenerator, snsWriter), actorSystem)
  }

  private def start(sqsReader: SQSReader,
                    idGenerator: IdentifierGenerator,
                    snsWriter: SNSWriter) = {

    sqsReader.retrieveMessages().map { messages =>
      messages.map { message =>
        for {
          unifiedItem <- UnifiedItemExtractor.toUnifiedItem(message)
          canonicalId <- idGenerator.generateId(unifiedItem)
          _ <- snsWriter.writeMessage(toIdentifiedUnifiedItemJson(unifiedItem,
                                                                  canonicalId),
                                      Some(snsSubject))
        } yield ()
      }
    }
  }

  private def toIdentifiedUnifiedItemJson(unifiedItem: UnifiedItem,
                                          canonicalId: String) = {
    JsonUtil.toJson(IdentifiedUnifiedItem(canonicalId, unifiedItem)).get
  }
}
