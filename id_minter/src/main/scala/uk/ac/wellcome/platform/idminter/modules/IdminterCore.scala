package uk.ac.wellcome.platform.idminter.modules

import uk.ac.wellcome.models.IdentifiedUnifiedItem
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.ExecutionContext.Implicits.global

class IdminterCore(sqsReader: SQSReader, unifiedItemExtractor: UnifiedItemExtractor, idGenerator: IdGenerator, snsWriter: SNSWriter) {
  def start() = {

   // Poller.runContinuously {
    sqsReader.retrieveMessage().map {
      case Some(message) => for {
        unifiedItem <- unifiedItemExtractor.toUnifiedItem(message)
        canonicalId <- idGenerator.generateId(unifiedItem)
        _ <- snsWriter.writeMessage(JsonUtil.toJson(IdentifiedUnifiedItem(canonicalId,unifiedItem)).get,None)
      } yield ()
      case None =>
    }
  //  }
  }
}
