package uk.ac.wellcome.platform.idminter.modules

import uk.ac.wellcome.models.UnifiedItem

import scala.concurrent.Future

class IdGenerator {
  def generateId(identifiers: UnifiedItem): Future[String] = ???

}
