package uk.ac.wellcome.platform.transformer.transformers

import org.scalatest.Matchers
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.models.work.internal.UnidentifiedWork

import scala.reflect.ClassTag

/** Utilities for transformer tests.
  *
  * @@AWLC: when I first wrote this code, I got the following error:
  *
  *     TransformableTestBase.scala:12:47: No ClassTag available for T
  *         val triedMaybeWork = transformer.transform(transformable, version = 1)
  *                                                    ↑
  *
  * The addition of the implicit ClassTag[T] is working around this error.
  * In general reflection is icky, but it's only in tests so I think this
  * is okay.
  */
trait TransformableTestBase[T <: Transformable] extends Matchers {

  val transformer: TransformableTransformer[T]

  def transformToWork(transformable: T)(implicit c: ClassTag[T]): UnidentifiedWork = {
    val triedMaybeWork = transformer.transform(transformable, version = 1)
    if (triedMaybeWork.isFailure) triedMaybeWork.failed.get.printStackTrace()
    triedMaybeWork.isSuccess shouldBe true
    triedMaybeWork.get.get
  }

  def assertTransformToWorkFails(transformable: T)(implicit c: ClassTag[T]): Unit = {
    transformer
      .transform(transformable, version = 1)
      .isSuccess shouldBe false
  }
}
