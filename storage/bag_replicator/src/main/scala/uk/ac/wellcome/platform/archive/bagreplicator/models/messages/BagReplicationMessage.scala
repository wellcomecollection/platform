package uk.ac.wellcome.platform.archive.bagreplicator.models.messages

import uk.ac.wellcome.platform.archive.common.models.ReplicationRequest
import uk.ac.wellcome.platform.archive.common.models.bagit.BagLocation

trait BagReplicationContext {
  val context: ReplicationRequest
}

case class InternalReplicationRequest(context: ReplicationRequest,
                                      sourceBagLocation: BagLocation)
    extends BagReplicationContext

case class CompletedBagReplication(context: ReplicationRequest,
                                   dstBagLocation: BagLocation)
    extends BagReplicationContext

case class PublishedToOutgoingTopic(context: ReplicationRequest)
    extends BagReplicationContext
