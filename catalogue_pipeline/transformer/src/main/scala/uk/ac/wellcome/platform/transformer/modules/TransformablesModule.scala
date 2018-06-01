package uk.ac.wellcome.platform.transformer.modules

import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton
import uk.ac.wellcome.models.transformable.{CalmTransformable, MiroTransformable, SierraTransformable}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend

import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext

object TransformablesModule extends TwitterModule{
  @Provides
  @Singleton
  def provideMiroTransformable(injector: Injector): ObjectStore[MiroTransformable] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    implicitly[ObjectStore[MiroTransformable]]
  }

  @Provides
  @Singleton
  def provideMiroCalmTransformable(injector: Injector): ObjectStore[CalmTransformable] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    implicitly[ObjectStore[CalmTransformable]]
  }

  @Provides
  @Singleton
  def provideSierraTransformable(injector: Injector): ObjectStore[SierraTransformable] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    implicitly[ObjectStore[SierraTransformable]]
  }
}
