package uk.ac.wellcome.messaging.sns

case class PublishAttempt(id: Either[Throwable, String])
