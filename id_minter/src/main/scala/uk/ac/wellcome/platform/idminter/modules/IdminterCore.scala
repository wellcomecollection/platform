package uk.ac.wellcome.platform.idminter.modules

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdminterCore(sqsReader: SQSReader, unifiedItemExtractor: UnifiedItemExtractor, idGenerator: IdGenerator, itemWrapper: ItemWrapper, sqsWriter: SQSWriter) {
  def start() = {

   // Poller.runContinuously {
    sqsReader.retrieveMessage().map {
      case Some(message) => for {
        unifiedItem <- unifiedItemExtractor.toUnifiedItem(message)
        canonicalId <- idGenerator.generateId(unifiedItem)
        wrappedItem <- itemWrapper.wrapItem(unifiedItem, canonicalId)
        _ <- sqsWriter.writeItem(wrappedItem)
      } yield ()
      case None =>
    }
  //  }
  }
}
