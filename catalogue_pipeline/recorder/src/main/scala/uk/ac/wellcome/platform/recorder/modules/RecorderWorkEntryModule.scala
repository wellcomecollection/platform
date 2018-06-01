package uk.ac.wellcome.platform.recorder.modules

import javax.inject.Singleton
import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext

object RecorderWorkEntryModule extends TwitterModule {
  @Provides
  @Singleton
  def provideUnidentifiedWorkStore(
    injector: Injector): ObjectStore[RecorderWorkEntry] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[RecorderWorkEntry]
  }
}
