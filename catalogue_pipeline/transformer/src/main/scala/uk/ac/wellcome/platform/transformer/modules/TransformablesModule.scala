package uk.ac.wellcome.platform.transformer.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import io.circe.{KeyDecoder, KeyEncoder}
import uk.ac.wellcome.models.transformable.sierra.SierraItemNumber
import uk.ac.wellcome.models.transformable.{
  MiroTransformable,
  SierraTransformable
}
import uk.ac.wellcome.platform.transformer.models.TransformerConfig
import uk.ac.wellcome.platform.transformer.models.TransformerSourceNames._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext

object TransformablesModule extends TwitterModule {
  private val sourceNameFlag = flag[String](
    "transformer.sourceName", "Name of the transformer source")

  @Provides
  @Singleton
  def providesTransformerConfig(): TransformerConfig = {
    val sourceName = sourceNameFlag() match {
      case s: String if s == miro.toString  => miro
      case s: String if s == sierra.toString => sierra
      case s: String =>
        throw new IllegalArgumentException(
          s"$s is not a valid transformer source name")
    }

    TransformerConfig(sourceName = sourceName)
  }

  @Provides
  @Singleton
  def provideMiroTransformableObjectStore(
    injector: Injector): ObjectStore[MiroTransformable] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[MiroTransformable]
  }

  implicit val keyDecoder: KeyDecoder[SierraItemNumber] =
    SierraTransformable.keyDecoder
  implicit val keyEncoder: KeyEncoder[SierraItemNumber] =
    SierraTransformable.keyEncoder

  @Provides
  @Singleton
  def provideSierraTransformableObjectStore(
    injector: Injector): ObjectStore[SierraTransformable] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[SierraTransformable]
  }
}
