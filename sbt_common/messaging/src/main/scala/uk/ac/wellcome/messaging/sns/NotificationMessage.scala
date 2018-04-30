package uk.ac.wellcome.messaging.sns

case class NotificationMessage(
  MessageId: String,
  TopicArn: String,
  Subject: String,
  Message: String
)
