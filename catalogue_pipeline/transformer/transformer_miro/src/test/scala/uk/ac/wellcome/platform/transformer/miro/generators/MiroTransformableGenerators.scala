package uk.ac.wellcome.platform.transformer.miro.generators

import uk.ac.wellcome.platform.transformer.miro.models.MiroTransformable

trait MiroTransformableGenerators {
  def createMiroTransformableWith(
    miroId: String = "M0000001",
    data: String
  ): MiroTransformable =
    MiroTransformable(
      sourceId = miroId,
      MiroCollection = "does-not-matter",
      data = data
    )
}
