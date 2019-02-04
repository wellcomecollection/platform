package uk.ac.wellcome.platform.archive.bagreplicator.models.messages

import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, FuzzyWuzzy}

trait BagReplicationContext {
  val context: ArchiveComplete
}

case class BagReplicationRequest(context: ArchiveComplete,
                                 sourceBagLocation: FuzzyWuzzy)
    extends BagReplicationContext

case class CompletedBagReplication(context: ArchiveComplete)
    extends BagReplicationContext

case class PublishedToOutgoingTopic(context: ArchiveComplete)
    extends BagReplicationContext
