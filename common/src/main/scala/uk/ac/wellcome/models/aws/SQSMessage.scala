package uk.ac.wellcome.models.aws

import com.fasterxml.jackson.annotation._
import io.circe.generic.extras.JsonKey

case class SQSMessage(
  @JsonKey("Subject") @JsonProperty("Subject") subject: Option[String],
  @JsonKey("Message") @JsonProperty("Message") body: String,
  @JsonKey("TopicArn") @JsonProperty("TopicArn") topic: String,
  @JsonKey("Type") @JsonProperty("Type") messageType: String,
  @JsonKey("Timestamp") @JsonProperty("Timestamp") timestamp: String
)
