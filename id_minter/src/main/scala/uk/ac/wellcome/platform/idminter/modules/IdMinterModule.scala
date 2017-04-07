package uk.ac.wellcome.platform.idminter.modules

import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, UnifiedItem}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.ExecutionContext.Implicits.global

object IdMinterModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    info("Starting SQS worker")

    val sqsReader = injector.instance[SQSReader]
    val idGenerator = injector.instance[IdGenerator]
    val snsWriter = injector.instance[SNSWriter]
    start(sqsReader, idGenerator,snsWriter)
  }

  def start(sqsReader: SQSReader, idGenerator: IdGenerator, snsWriter: SNSWriter) = {

    // Poller.runContinuously {
    sqsReader.retrieveMessage().map {
      case Some(message) => for {
        unifiedItem <- UnifiedItemExtractor.toUnifiedItem(message)
        canonicalId <- idGenerator.generateId(unifiedItem)
        _ <- snsWriter.writeMessage(toIdentifiedUnifiedItemJson(unifiedItem, canonicalId),None)
      } yield ()
      case None =>
    }
    //  }
  }

  private def toIdentifiedUnifiedItemJson(unifiedItem: UnifiedItem, canonicalId: String) = {
    JsonUtil.toJson(IdentifiedUnifiedItem(canonicalId, unifiedItem)).get
  }
}
