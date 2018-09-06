package uk.ac.wellcome.messaging.message

import uk.ac.wellcome.storage.ObjectLocation

sealed trait MessageNotification

/** Represents a message where the body of the message is stored in a
  * remote location (for example, an object in S3).
  */
case class RemoteNotification(location: ObjectLocation) extends MessageNotification

/** Represents a message where the body of the message is stored inline.
  * The type [[T]] is the type of the case class that was originally
  * serialised.
  */
case class InlineNotification[T](t: T) extends MessageNotification
