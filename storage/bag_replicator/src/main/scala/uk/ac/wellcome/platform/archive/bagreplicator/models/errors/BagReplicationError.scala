package uk.ac.wellcome.platform.archive.bagreplicator.models.errors

import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete

trait ErrorMessage {
  val errorMessage: String
}

trait BagReplicationContext {
  val context: ArchiveComplete
}

abstract class BagReplicationError extends ErrorMessage

case class NotificationParsingFailed(errorMessage: String)
  extends BagReplicationError

case class DuplicationFailed(errorMessage: String, context: ArchiveComplete)
  extends BagReplicationError with BagReplicationContext

case class NotificationFailed(errorMessage: String, context: ArchiveComplete)
  extends BagReplicationError with BagReplicationContext