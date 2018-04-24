package uk.ac.wellcome.sns

case class NotificationMessage(
  MessageId: String,
  TopicArn: String,
  Subject: String,
  Message: String
)
