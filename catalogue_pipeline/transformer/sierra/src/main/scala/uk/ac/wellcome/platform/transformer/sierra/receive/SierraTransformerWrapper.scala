package uk.ac.wellcome.platform.transformer.sierra.receive

import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.sierra.SierraTransformableTransformer

import scala.concurrent.Future
import scala.util.Try

class SierraTransformerWrapper @Inject()(
  sierraTransformableTransformer: SierraTransformableTransformer) extends Logging{

  def transformToWork(transformableRecord: SierraTransformable, version: Int): Future[TransformedBaseWork] = {
    Future.fromTry(
      transformTransformable(transformableRecord, version))
  }

  private def transformTransformable(
    transformable: SierraTransformable,
    version: Int
  ): Try[TransformedBaseWork] = {
    sierraTransformableTransformer.transform(transformable, version) map {
      transformed =>
        debug(s"Transformed record to $transformed")
        transformed
    } recover {
      case e: Throwable =>
        error("Failed to perform transform to unified item", e)
        throw e
    }
  }
}
