package uk.ac.wellcome.sns

case class NotificationMessage(
  MessageId: String,
  TopicArn: String,
  Subject: String,
  Message: String,
  Timestamp: String,
  SignatureVersion: String,
  Signature: String,
  SigningCertURL: String,
  UnsubscribeURL: String
)
