package uk.ac.wellcome.platform.merger.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.work.internal.BaseWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext

object BaseWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideBaseWorkStore(injector: Injector): ObjectStore[BaseWork] = {
    implicit val storageBackend: S3StorageBackend =
      injector.instance[S3StorageBackend]
    implicit val executionContext: ExecutionContext =
      injector.instance[ExecutionContext]

    ObjectStore[BaseWork]
  }
}
