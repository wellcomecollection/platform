package uk.ac.wellcome.models.aws

import com.fasterxml.jackson.annotation._

case class SQSMessage(
  @JsonProperty("Subject") subject: Option[String],
  @JsonProperty("Message") body: String,
  @JsonProperty("TopicArn") topic: String,
  @JsonProperty("Type") messageType: String,
  @JsonProperty("Timestamp") timestamp: String
)
