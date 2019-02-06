package uk.ac.wellcome.platform.archive.bagreplicator.models.errors

import uk.ac.wellcome.platform.archive.common.models.ReplicationRequest

trait ErrorMessage {
  val errorMessage: String
}

trait BagReplicationErrorContext {
  val context: ReplicationRequest
}

abstract class BagReplicationError extends ErrorMessage

case class NotificationParsingFailed(errorMessage: String)
    extends BagReplicationError

case class DuplicationFailed(errorMessage: String, context: ReplicationRequest)
    extends BagReplicationError
    with BagReplicationErrorContext

case class NotificationFailed(errorMessage: String, context: ReplicationRequest)
    extends BagReplicationError
    with BagReplicationErrorContext
