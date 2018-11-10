package uk.ac.wellcome.sierra_adapter.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.storage.builders.{
  DynamoBuilder,
  S3Builder,
  VHSBuilder
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.ExecutionContext

object SierraTransformableVHSBuilder {
  type SierraVHS = VersionedHybridStore[SierraTransformable,
                                        EmptyMetadata,
                                        ObjectStore[SierraTransformable]]
  def buildSierraVHS(config: Config): SierraVHS = {
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    implicit val storageBackend: S3StorageBackend = new S3StorageBackend(
      s3Client = S3Builder.buildS3Client(config)
    )

    new SierraVHS(
      vhsConfig = VHSBuilder.buildVHSConfig(config),
      objectStore = ObjectStore[SierraTransformable],
      dynamoDbClient = DynamoBuilder.buildDynamoClient(config)
    )
  }
}
