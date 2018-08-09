package uk.ac.wellcome.platform.ingestor.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext

object IdentifiedBaseWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideUnidentifiedWorkStore(
    injector: Injector): ObjectStore[IdentifiedBaseWork] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[IdentifiedBaseWork]
  }
}
