package uk.ac.wellcome.models.aws

import io.circe.generic.extras.JsonKey

// Deprecated in favour of uk.ac.wellcome.sns.NotificationMessage
case class SQSMessage(
  @JsonKey("Subject") subject: Option[String],
  @JsonKey("Message") body: String,
  @JsonKey("TopicArn") topic: String,
  @JsonKey("Type") messageType: String,
  @JsonKey("Timestamp") timestamp: String
)
