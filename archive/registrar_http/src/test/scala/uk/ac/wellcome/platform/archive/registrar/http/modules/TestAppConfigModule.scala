package uk.ac.wellcome.platform.archive.registrar.http.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.common.config.models.OldHttpServerConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.common.modules.HybridStoreConfig
import uk.ac.wellcome.platform.archive.registrar.http.config.RegistrarHttpConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

class TestAppConfigModule(
                           serverConfig: OldHttpServerConfig,
                           hybridStoreTableName: String,
                           hybridStoreBucketName: String,
                           hybridStoreGlobalPrefix: String
) extends AbstractModule {

  @Provides
  def providesAppConfig = {

    val s3ClientConfig = S3ClientConfig(
      accessKey = Some("accessKey1"),
      secretKey = Some("verySecretKey1"),
      region = "localhost",
      endpoint = Some("http://localhost:33333")
    )
    val hybridStoreConfig = HybridStoreConfig(
      dynamoClientConfig = DynamoClientConfig(
        accessKey = Some("access"),
        secretKey = Some("secret"),
        region = "localhost",
        endpoint = Some("http://localhost:45678")
      ),
      s3ClientConfig = s3ClientConfig,
      dynamoConfig = DynamoConfig(
        table = hybridStoreTableName,
        maybeIndex = None
      ),
      s3Config = S3Config(
        bucketName = hybridStoreBucketName
      ),
      s3GlobalPrefix = hybridStoreGlobalPrefix
    )

    RegistrarHttpConfig(
      hybridStoreConfig,
      serverConfig
    )
  }
}
