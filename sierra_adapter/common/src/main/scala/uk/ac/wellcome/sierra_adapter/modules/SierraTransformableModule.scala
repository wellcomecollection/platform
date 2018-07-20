package uk.ac.wellcome.sierra_adapter.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext

object SierraTransformableModule extends TwitterModule {
  @Provides
  @Singleton
  def provideSierraTransformableObjectStore(injector: Injector): ObjectStore[SierraTransformable] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]
    implicit val serialisationStrategy =
      injector.instance[SerialisationStrategy[SierraTransformable]]

    ObjectStore.createObjectStore[SierraTransformable, S3StorageBackend](
      storageStrategy = serialisationStrategy,
      storageBackend = storageBackend,
      ec = executionContext
    )
  }
}
