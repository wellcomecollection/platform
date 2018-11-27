package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Assertion, Matchers, Suite}
import uk.ac.wellcome.models.work.internal.{
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.miro.MiroTransformableTransformer
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

import scala.util.Try

trait MiroTransformableWrapper extends Matchers { this: Suite =>
  val transformer = new MiroTransformableTransformer

  def transformWork(miroRecord: MiroRecord): UnidentifiedWork =
    transformToWork(miroRecord).asInstanceOf[UnidentifiedWork]

  def assertTransformWorkFails(miroRecord: MiroRecord): Assertion =
    transformer
      .transform(miroRecord, version = 1)
      .isSuccess shouldBe false

  private def transformToWork(miroRecord: MiroRecord): TransformedBaseWork = {
    val triedWork: Try[TransformedBaseWork] =
      transformer.transform(miroRecord, version = 1)

    if (triedWork.isFailure) {
      triedWork.failed.get.printStackTrace()
      println(
        triedWork.failed.get.asInstanceOf[TransformerException].e.getMessage)
    }

    triedWork.isSuccess shouldBe true
    triedWork.get
  }
}
