package uk.ac.wellcome.messaging.message

import java.net.URI

import uk.ac.wellcome.s3.S3ObjectLocation

import scala.util.Try

case class MessagePointer(src: S3ObjectLocation)
