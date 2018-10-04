package uk.ac.wellcome.platform.archive.registrar.modules

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.registrar.models.BagManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend

object VHSModule extends AbstractModule {
  @Provides
  def providesBagManifestObjectStore(s3StorageBackend: S3StorageBackend,
                                     actorSystem: ActorSystem) = {
    implicit val executionContext = actorSystem.dispatcher
    implicit val storageBackend = s3StorageBackend

    ObjectStore[BagManifest]
  }
}
