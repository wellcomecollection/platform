package uk.ac.wellcome.platform.ingestor.modules

import javax.inject.Singleton
import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext

object IdentifiedWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideUnidentifiedWorkStore(injector: Injector): ObjectStore[IdentifiedWork] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    implicitly[ObjectStore[IdentifiedWork]]
  }
}
