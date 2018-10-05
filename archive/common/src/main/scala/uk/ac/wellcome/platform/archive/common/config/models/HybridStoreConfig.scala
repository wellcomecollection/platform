package uk.ac.wellcome.platform.archive.common.config.models

import uk.ac.wellcome.platform.archive.common.modules.{
  DynamoClientConfig,
  S3ClientConfig
}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

case class HybridStoreConfig(
  dynamoClientConfig: DynamoClientConfig,
  s3ClientConfig: S3ClientConfig,
  dynamoConfig: DynamoConfig,
  s3Config: S3Config,
  s3GlobalPrefix: String
)
