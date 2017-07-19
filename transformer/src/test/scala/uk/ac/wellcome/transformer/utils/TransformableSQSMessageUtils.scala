package uk.ac.wellcome.transformer.utils

import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{CalmTransformable, MiroTransformable}
import uk.ac.wellcome.utils.JsonUtil

trait TransformableSQSMessageUtils {

  def createValidCalmSQSMessage(RecordID: String,
                                RecordType: String,
                                AltRefNo: String,
                                RefNo: String,
                                data: String): SQSMessage = {
    val calmTransformable =
      CalmTransformable(RecordID, RecordType, AltRefNo, RefNo, data)

    sqsMessage(JsonUtil.toJson(calmTransformable).get)
  }

  def createValidMiroRecord(MiroID: String,
                            MiroCollection: String,
                            data: String): SQSMessage = {
    val miroTransformable = MiroTransformable(MiroID, MiroCollection, data)
    val value = JsonUtil.toJson(miroTransformable).get
    sqsMessage(value)
  }

  def createInvalidRecord: SQSMessage = sqsMessage("not a json string")

  private def sqsMessage(message: String) = {
    SQSMessage(None, message, "test-calm_transformer_topic", "notification", "")
  }
}
