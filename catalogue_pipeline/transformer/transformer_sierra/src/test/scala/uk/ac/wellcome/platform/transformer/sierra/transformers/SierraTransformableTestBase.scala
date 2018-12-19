package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.Matchers
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.sierra.SierraTransformableTransformer
import uk.ac.wellcome.platform.transformer.sierra.exceptions.SierraTransformerException

import scala.util.Try

trait SierraTransformableTestBase extends Matchers {

  val transformer: SierraTransformableTransformer

  def transformToWork(
    transformable: SierraTransformable): TransformedBaseWork = {
    val triedWork: Try[TransformedBaseWork] =
      transformer.transform(transformable, version = 1)

    if (triedWork.isFailure) {
      triedWork.failed.get.printStackTrace()
      println(
        triedWork.failed.get
          .asInstanceOf[SierraTransformerException]
          .e
          .getMessage)
    }

    triedWork.isSuccess shouldBe true
    triedWork.get
  }

  def assertTransformToWorkFails(transformable: SierraTransformable): Unit = {
    transformer
      .transform(transformable, version = 1)
      .isSuccess shouldBe false
  }
}
