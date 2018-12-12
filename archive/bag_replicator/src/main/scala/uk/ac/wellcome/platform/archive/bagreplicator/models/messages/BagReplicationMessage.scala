package uk.ac.wellcome.platform.archive.bagreplicator.models.messages

import uk.ac.wellcome.platform.archive.common.models.BagLocation

trait BagReplicationContext[T] {
  val context: T
}
case class BagReplicationRequest[T](context: T, sourceBagLocation: BagLocation) extends BagReplicationContext[T]

case class CompletedBagReplication[T](context: T) extends BagReplicationContext[T]
