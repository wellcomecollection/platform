package uk.ac.wellcome.platform.transformer.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import io.circe.{KeyDecoder, KeyEncoder}
import uk.ac.wellcome.finatra.akka.AkkaModule
import uk.ac.wellcome.models.transformable.sierra.SierraItemNumber
import uk.ac.wellcome.models.transformable.{
  MiroTransformable,
  SierraTransformable,
  Transformable
}
import uk.ac.wellcome.platform.transformer.models.{TransformerConfig, TransformerSourceNames}
import uk.ac.wellcome.platform.transformer.models.TransformerSourceNames._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.transformer.services.TransformerWorkerService
import uk.ac.wellcome.platform.transformer.transformers.{MiroTransformableTransformer, SierraTransformableTransformer, TransformableTransformer}

import scala.concurrent.{ExecutionContext, Future}

object TransformablesModule extends TwitterModule {
  override val modules = Seq(AkkaModule)

  private val sourceNameFlag = flag[String](
    "transformer.sourceName", help = "Name of the transformer source")

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

  implicit val keyDecoder: KeyDecoder[SierraItemNumber] =
    SierraTransformable.keyDecoder
  implicit val keyEncoder: KeyEncoder[SierraItemNumber] =
    SierraTransformable.keyEncoder

  @Provides
  @Singleton
  def providesObjectStore(injector: Injector, transformerConfig: TransformerConfig): ObjectStore[_ <: Transformable] = {
    implicit val storageBackend: S3StorageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext: ExecutionContext = injector.instance[ExecutionContext]

    transformerConfig.sourceName match {
      case TransformerSourceNames.miro   => ObjectStore[MiroTransformable]
      case TransformerSourceNames.sierra => ObjectStore[SierraTransformable]
    }
  }

  @Provides
  @Singleton
  def providesTransformableTransformer(transformerConfig: TransformerConfig): TransformableTransformer[_ <: Transformable] =
    transformerConfig.sourceName match {
      case TransformerSourceNames.miro   => new MiroTransformableTransformer
      case TransformerSourceNames.sierra => new SierraTransformableTransformer
    }

  @Provides
  @Singleton
  def providesTransformableWorkerService(injector: Injector, transformerConfig: TransformerConfig): TransformerWorkerService[_ <: Transformable] =
    transformerConfig.sourceName match {
      case TransformerSourceNames.miro   => injector.instance[TransformerWorkerService[MiroTransformable]]
      case TransformerSourceNames.sierra => injector.instance[TransformerWorkerService[SierraTransformable]]
    }
}
