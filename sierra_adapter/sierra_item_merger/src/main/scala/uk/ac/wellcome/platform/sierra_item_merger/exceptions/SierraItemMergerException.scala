package uk.ac.wellcome.platform.sierra_item_merger.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class SierraItemMergerException(e: Throwable) extends GracefulFailureException

case object SierraItemMergerException {
  def apply(message: String): SierraItemMergerException =
    SierraItemMergerException(new RuntimeException(message))
}
