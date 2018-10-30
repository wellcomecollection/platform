package uk.ac.wellcome.platform.transformer.miro.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.transformer.miro.models.MiroTransformable
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend

import scala.concurrent.ExecutionContext

object MiroTransformableModule extends TwitterModule {
  @Provides
  @Singleton
  def provideMiroTransformableObjectStore(
    injector: Injector): ObjectStore[MiroTransformable] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[MiroTransformable]
  }
}
