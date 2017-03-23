package uk.ac.wellcome.models

import scala.util.Try

case class PublishAttempt(id: String)

trait PublishableMessage extends Message {
  def publish(): Try[PublishAttempt]
}
