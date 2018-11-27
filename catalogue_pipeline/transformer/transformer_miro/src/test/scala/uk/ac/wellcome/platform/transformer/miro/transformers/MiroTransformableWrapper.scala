package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Assertion, Matchers, Suite}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{TransformedBaseWork, UnidentifiedWork}
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.miro.MiroTransformableTransformer
import uk.ac.wellcome.platform.transformer.miro.generators.MiroTransformableGenerators
import uk.ac.wellcome.platform.transformer.miro.models.{MiroMetadata, MiroTransformable}
import uk.ac.wellcome.platform.transformer.miro.source.MiroTransformableData

import scala.util.Try

/** MiroTransformable looks for several fields in the source JSON -- if they're
  *  missing or have the wrong values, it rejects the record.
  *
  *  This trait provides a single method `transform()` which adds the necessary
  *  fields before transformation, allowing tests to focus on only the fields
  *  that are interesting for that test.
  */
trait MiroTransformableWrapper
    extends Matchers
    with MiroTransformableGenerators { this: Suite =>

  val transformer = new MiroTransformableTransformer

  def transformWork(
    miroId: String = "M0000001",
    data: String = ""
  ): UnidentifiedWork = {
    val data = buildJSONForWork(miroId = miroId, data)
    val transformable = fromJson[MiroTransformableData](data).get

    transformToWork(transformable = transformable).asInstanceOf[UnidentifiedWork]
  }

  def assertTransformWorkFails(transformable: MiroTransformableData): Assertion =
    transformer
      .transform(transformable, metadata = MiroMetadata(isClearedForCatalogueAPI = true), version = 1)
      .isSuccess shouldBe false

  private def transformToWork(transformable: MiroTransformableData): TransformedBaseWork = {
    val triedWork: Try[TransformedBaseWork] = transformer.transform(
      transformable,
      metadata = MiroMetadata(isClearedForCatalogueAPI = true),
      version = 1
    )

    if (triedWork.isFailure) {
      triedWork.failed.get.printStackTrace()
      println(
        triedWork.failed.get.asInstanceOf[TransformerException].e.getMessage)
    }

    triedWork.isSuccess shouldBe true
    triedWork.get
  }
}
