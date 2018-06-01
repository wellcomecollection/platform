package uk.ac.wellcome.platform.recorder.modules

import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext

object UnidentifiedWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideUnidentifiedWorkStore(injector: Injector): ObjectStore[UnidentifiedWork] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    implicitly[ObjectStore[UnidentifiedWork]]
  }
}
