package uk.ac.wellcome.platform.archive.registrar.http.config

import java.net.URL

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.common.modules.HybridStoreConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

class RegistrarHttpArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments) {

  private val appPort =
    opt[Int]("app-port", required = true, default = Some(9001))
  private val appHost =
    opt[String]("app-host", required = true, default = Some("0.0.0.0"))
  private val appBaseUrl =
    opt[String]("app-base-url", required = true)
  private val contextUrl =
    opt[URL]("context-url", required = true)

  val hybridDynamoAccessKey = opt[String]("hybrid-dynamo-access-key")
  val hybridDynamoSecretKey = opt[String]("hybrid-dynamo-secret-key")
  val hybridDynamoRegion =
    opt[String]("hybrid-dynamo-region", default = Some("eu-west-1"))
  val hybridDynamoEndpoint = opt[String]("hybrid-dynamo-endpoint")

  val hybridS3AccessKey = opt[String]("hybrid-s3-access-key")
  val hybridS3SecretKey = opt[String]("hybrid-s3-secret-key")
  val hybridS3Region =
    opt[String]("hybrid-s3-region", default = Some("eu-west-1"))
  val hybridS3Endpoint = opt[String]("hybrid-s3-endpoint")

  val hybridGlobalS3Prefix =
    opt[String]("hybrid-global-s3-prefix", default = Some("archive"))
  val hybridDynamoTableName = opt[String]("hybrid-dynamo-table-name")
  val hybridS3BucketName = opt[String]("hybrid-s3-bucket-name")

  verify()

  val httpServerConfig = HttpServerConfig(
    host = appHost(),
    port = appPort(),
    externalBaseUrl = appBaseUrl(),
    contextUrl = contextUrl()
  )

  val hybridStoreConfig = HybridStoreConfig(
    dynamoClientConfig = DynamoClientConfig(
      accessKey = hybridS3AccessKey.toOption,
      secretKey = hybridS3SecretKey.toOption,
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

  val appConfig = RegistrarHttpConfig(
    hybridStoreConfig,
    httpServerConfig
  )
}
