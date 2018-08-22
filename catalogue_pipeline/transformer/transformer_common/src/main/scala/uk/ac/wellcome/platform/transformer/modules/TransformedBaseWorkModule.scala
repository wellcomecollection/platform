package uk.ac.wellcome.platform.transformer.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext

object TransformedBaseWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideTransformedBaseWorkStore(
    injector: Injector): ObjectStore[TransformedBaseWork] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    implicitly[ObjectStore[TransformedBaseWork]]
  }
}
