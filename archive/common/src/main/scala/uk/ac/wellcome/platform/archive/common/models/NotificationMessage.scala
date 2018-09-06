package uk.ac.wellcome.platform.archive.common.models

case class NotificationMessage(
  MessageId: String,
  TopicArn: String,
  Subject: Option[String],
  Message: String
)
