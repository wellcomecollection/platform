package uk.ac.wellcome.messaging.message

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.models.aws.S3Config

case class MessageConfig(
  snsConfig: SNSConfig,
  s3Config: S3Config
)
