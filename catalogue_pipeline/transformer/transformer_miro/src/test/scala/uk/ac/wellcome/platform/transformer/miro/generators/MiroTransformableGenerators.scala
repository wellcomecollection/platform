package uk.ac.wellcome.platform.transformer.miro.generators

import uk.ac.wellcome.platform.transformer.miro.models.MiroTransformable
import uk.ac.wellcome.platform.transformer.miro.source.MiroTransformableData

trait MiroTransformableGenerators {
  def createMiroTransformableWith(
    miroId: String = "M0000001",
    data: String
  ): MiroTransformable =
    MiroTransformable(
      sourceId = miroId,
      data = data
    )

  def createMiroTransformableDataWith(innopacID: Option[String] = None, useRestrictions: Option[String] = None, sourceCode: Option[String] = None): MiroTransformableData =
    MiroTransformableData(

      miroId = "M0000001",
      innopacID = innopacID,
      useRestrictions = useRestrictions,
      sourceCode = sourceCode
    )

  def createMiroTransformableData: MiroTransformableData =
    createMiroTransformableDataWith()
}
