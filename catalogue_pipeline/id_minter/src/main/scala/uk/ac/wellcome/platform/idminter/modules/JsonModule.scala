package uk.ac.wellcome.platform.idminter.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import io.circe.Json
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend

import scala.concurrent.ExecutionContext

object JsonModule extends TwitterModule {
  @Provides
  @Singleton
  def provideJsonStore(injector: Injector): ObjectStore[Json] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[Json]
  }
}
