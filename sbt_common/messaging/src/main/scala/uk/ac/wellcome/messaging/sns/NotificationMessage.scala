package uk.ac.wellcome.messaging.sns

case class NotificationMessage(
  TopicArn: String,
  Subject: String,
  Message: String
)
