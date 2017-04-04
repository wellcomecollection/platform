package uk.ac.wellcome.platform.idminter.modules

import scala.concurrent.Future


class SQSWriter {
  def writeItem(wrappedItem: UnifiedItemWrapper): Future[Unit] = ???

}
