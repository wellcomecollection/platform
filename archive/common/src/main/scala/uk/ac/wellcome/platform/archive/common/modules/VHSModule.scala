package uk.ac.wellcome.platform.archive.common.modules

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.common.config.models.HybridStoreConfig
import uk.ac.wellcome.storage.s3.{S3ClientFactory, S3StorageBackend}
import uk.ac.wellcome.storage.vhs.VHSConfig

object VHSModule extends AbstractModule {
  @Provides
  def providesVHSConfig(hybridStoreConfig: HybridStoreConfig) = {
    VHSConfig(
      dynamoConfig = hybridStoreConfig.dynamoConfig,
      s3Config = hybridStoreConfig.s3Config,
      globalS3Prefix = hybridStoreConfig.s3GlobalPrefix
    )
  }

  @Provides
  def providesS3StorageBackend(actorSystem: ActorSystem,
                               hybridStoreConfig: HybridStoreConfig) = {
    val s3Client = S3ClientFactory.create(
      region = hybridStoreConfig.s3ClientConfig.region,
      endpoint = hybridStoreConfig.s3ClientConfig.endpoint.getOrElse(""),
      accessKey = hybridStoreConfig.s3ClientConfig.accessKey.getOrElse(""),
      secretKey = hybridStoreConfig.s3ClientConfig.secretKey.getOrElse("")
    )
    implicit val executionContext = actorSystem.dispatcher
    new S3StorageBackend(s3Client)(executionContext)
  }
}

