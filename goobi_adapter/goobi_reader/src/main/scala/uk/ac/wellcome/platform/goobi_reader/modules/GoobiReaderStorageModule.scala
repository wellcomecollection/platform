package uk.ac.wellcome.platform.goobi_reader.modules

import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend

import scala.concurrent.ExecutionContext
import scala.tools.nsc.interpreter.InputStream

object GoobiReaderStorageModule extends TwitterModule {
  @Provides
  @Singleton
  def provideUnidentifiedWorkStore(
    injector: Injector): ObjectStore[InputStream] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[InputStream]
  }
}
