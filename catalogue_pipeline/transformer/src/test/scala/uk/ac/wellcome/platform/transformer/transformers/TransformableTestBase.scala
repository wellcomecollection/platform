package uk.ac.wellcome.platform.transformer.transformers

import org.scalatest.Matchers
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.models.work.internal.TransformedBaseWork

import scala.util.Try

trait TransformableTestBase[T <: Transformable] extends Matchers {

  val transformer: TransformableTransformer[T]

  def transformToWork(transformable: T): TransformedBaseWork = {
    val triedWork: Try[TransformedBaseWork] = transformer.transform(transformable, version = 1)
    if (triedWork.isFailure) triedWork.failed.get.printStackTrace()
    triedWork.isSuccess shouldBe true
    triedWork.get
  }

  def assertTransformToWorkFails(transformable: T): Unit = {
    transformer
      .transform(transformable, version = 1)
      .isSuccess shouldBe false
  }
}
