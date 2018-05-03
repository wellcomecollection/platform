package uk.ac.wellcome.messaging.message

import uk.ac.wellcome.storage.s3.S3ObjectLocation

case class MessagePointer(src: S3ObjectLocation)
