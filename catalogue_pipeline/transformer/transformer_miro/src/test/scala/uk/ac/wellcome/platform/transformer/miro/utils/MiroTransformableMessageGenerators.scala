package uk.ac.wellcome.platform.transformer.miro.utils

import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.models.work.test.util.IdentifiersGenerators

trait MiroTransformableMessageGenerators extends IdentifiersGenerators {
  def createValidMiroTransformableWith(miroId: String = "MiroId",
                                       miroCollection: String = "Collection-A",
                                       data: String =
                                         """
      |{
      |  "image_cleared": "Y",
      |  "image_copyright_cleared": "Y",
      |  "image_tech_file_size": ["1000000"],
      |  "image_use_restrictions": "CC-BY"
      |}
    """.stripMargin): MiroTransformable =
    MiroTransformable(
      sourceId = miroId,
      MiroCollection = miroCollection,
      data = data
    )
}
