package uk.ac.wellcome.platform.sierra_item_merger.exceptions

case class SierraItemMergerException(e: Throwable)
    extends Exception(e.getMessage)

case object SierraItemMergerException {
  def apply(message: String): SierraItemMergerException =
    SierraItemMergerException(new RuntimeException(message))
}
