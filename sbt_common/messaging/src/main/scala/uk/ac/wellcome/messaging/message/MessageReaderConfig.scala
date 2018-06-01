package uk.ac.wellcome.messaging.message

import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.storage.s3.S3Config

case class MessageReaderConfig(sqsConfig: SQSConfig, s3Config: S3Config)
