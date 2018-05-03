package uk.ac.wellcome.storage.vhs

import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

case class VHSConfig(
  dynamoConfig: DynamoConfig,
  s3Config: S3Config
)
