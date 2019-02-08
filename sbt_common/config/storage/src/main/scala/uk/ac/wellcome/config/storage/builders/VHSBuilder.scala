package uk.ac.wellcome.config.storage.builders

import com.typesafe.config.Config
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.storage.type_classes.SerialisationStrategy
import uk.ac.wellcome.storage.vhs.{VHSConfig, VersionedHybridStore}
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext

object VHSBuilder {
  def buildVHSConfig(config: Config): VHSConfig = {
    val s3Config = S3Builder.buildS3Config(config, namespace = "vhs")
    val dynamoConfig =
      DynamoBuilder.buildDynamoConfig(config, namespace = "vhs")

    val globalS3Prefix = config
      .getOrElse[String]("aws.vhs.s3.globalPrefix")(default = "")

    VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config,
      globalS3Prefix = globalS3Prefix
    )
  }

  def buildVHS[T, M](config: Config)(
    implicit serialisationStrategy: SerialisationStrategy[T])
    : VersionedHybridStore[T, M, ObjectStore[T]] = {
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    implicit val storageBackend: S3StorageBackend = new S3StorageBackend(
      s3Client = S3Builder.buildS3Client(config)
    )

    new VersionedHybridStore[T, M, ObjectStore[T]](
      vhsConfig = buildVHSConfig(config),
      objectStore = ObjectStore[T],
      dynamoDbClient = DynamoBuilder.buildDynamoClient(config)
    )
  }
}
