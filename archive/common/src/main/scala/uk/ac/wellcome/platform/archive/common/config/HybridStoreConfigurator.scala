package uk.ac.wellcome.platform.archive.common.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.common.config.models.HybridStoreConfig
import uk.ac.wellcome.platform.archive.common.modules.{
  DynamoClientConfig,
  S3ClientConfig
}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

trait HybridStoreConfigurator extends ScallopConf {
  val arguments: Seq[String]

  val hybridDynamoAccessKey = opt[String]()
  val hybridDynamoSecretKey = opt[String]()
  val hybridDynamoRegion = opt[String](default = Some("eu-west-1"))
  val hybridDynamoEndpoint = opt[String]()

  val hybridS3AccessKey = opt[String]()
  val hybridS3SecretKey = opt[String]()
  val hybridS3Region = opt[String](default = Some("eu-west-1"))
  val hybridS3Endpoint = opt[String]()

  val hybridGlobalS3Prefix = opt[String](default = Some("archive"))
  val hybridDynamoTableName = opt[String]()
  val hybridS3BucketName = opt[String]()

  verify()

  val hybridStoreConfig = HybridStoreConfig(
    dynamoClientConfig = DynamoClientConfig(
      accessKey = hybridDynamoAccessKey.toOption,
      secretKey = hybridDynamoSecretKey.toOption,
      region = hybridDynamoRegion(),
      endpoint = hybridDynamoEndpoint.toOption
    ),
    s3ClientConfig = S3ClientConfig(
      accessKey = hybridS3AccessKey.toOption,
      secretKey = hybridS3SecretKey.toOption,
      region = hybridS3Region(),
      endpoint = hybridS3Endpoint.toOption
    ),
    dynamoConfig = DynamoConfig(
      table = hybridDynamoTableName(),
      maybeIndex = None
    ),
    s3Config = S3Config(
      bucketName = hybridS3BucketName()
    ),
    s3GlobalPrefix = hybridGlobalS3Prefix()
  )
}
