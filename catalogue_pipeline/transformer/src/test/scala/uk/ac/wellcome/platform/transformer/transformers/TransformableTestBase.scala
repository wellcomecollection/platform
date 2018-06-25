package uk.ac.wellcome.platform.transformer.transformers

import org.scalatest.Matchers
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.models.work.internal.UnidentifiedWork

trait TransformableTestBase[T <: Transformable] extends Matchers {

  val transformer: TransformableTransformer[T]

  def transformToWork(transformable: T): UnidentifiedWork = {
    val triedMaybeWork = transformer.transform(transformable, version = 1)
    if (triedMaybeWork.isFailure) triedMaybeWork.failed.get.printStackTrace()
    triedMaybeWork.isSuccess shouldBe true
    triedMaybeWork.get.get
  }

  def assertTransformToWorkFails(transformable: T): Unit = {
    transformer
      .transform(transformable, version = 1)
      .isSuccess shouldBe false
  }
}
