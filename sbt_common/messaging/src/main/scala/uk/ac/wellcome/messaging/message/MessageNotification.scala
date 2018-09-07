package uk.ac.wellcome.messaging.message

import uk.ac.wellcome.storage.ObjectLocation

/** Most of our inter-application messages are fairly small, and can be
  * sent inline on SNS/SQS.  But occasionally, we have an unusually large
  * message that can't be sent this way.  In that case, we store a copy
  * of the message in S3, and send an ObjectLocation telling the consumer
  * where to find the main message.
  *
  * This trait is the base for the two notification classes -- so we
  * can do `fromJson[MessageNotification]` in the consumer, and get the
  * correct type back.
  */
sealed trait MessageNotification[T]

/** Represents a message where the body of the message is stored in a
  * remote location (for example, an object in S3).
  */
case class RemoteNotification[T](location: ObjectLocation)
    extends MessageNotification[T]

/** Represents a message where the body of the message is stored inline.
  * The type [[T]] is the type of the case class that was originally
  * serialised.
  */
case class InlineNotification[T](body: T) extends MessageNotification[T]
