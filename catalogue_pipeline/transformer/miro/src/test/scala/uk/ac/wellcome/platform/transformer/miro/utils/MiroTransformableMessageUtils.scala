package uk.ac.wellcome.platform.transformer.miro.utils

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil

trait MiroTransformableMessageUtils extends IdentifiersUtil with SQS {
  def createValidMiroTransformableJson(MiroID: String= "MiroId",
                                       MiroCollection: String = "Collection-A",
                                       data: String = "{}"): String = {
    val miroTransformable =
      MiroTransformable(
        sourceId = MiroID,
        MiroCollection = MiroCollection,
        data = data
      )

    toJson(miroTransformable).get
  }
}
