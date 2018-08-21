package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.Matchers
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.miro.transformers.TransformableTransformer

import scala.util.Try

trait TransformableTestBase[T <: Transformable] extends Matchers {

  val transformer: TransformableTransformer[T]

  def transformToWork(transformable: T): TransformedBaseWork = {
    val triedWork: Try[TransformedBaseWork] =
      transformer.transform(transformable, version = 1)

    if (triedWork.isFailure) {
      triedWork.failed.get.printStackTrace()
      println(
        triedWork.failed.get.asInstanceOf[TransformerException].e.getMessage)
    }

    triedWork.isSuccess shouldBe true
    triedWork.get
  }

  def assertTransformToWorkFails(transformable: T): Unit = {
    transformer
      .transform(transformable, version = 1)
      .isSuccess shouldBe false
  }
}
