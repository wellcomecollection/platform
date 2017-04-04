package uk.ac.wellcome.platform.idminter.modules

import uk.ac.wellcome.models.UnifiedItem

import scala.concurrent.Future

class ItemWrapper {
  def wrapItem(message: UnifiedItem, canonicalId: String): Future[UnifiedItemWrapper] = ???

}

case class UnifiedItemWrapper(unifiedItem: UnifiedItem)
