package uk.ac.wellcome.transformer.utils

import java.time.Instant

import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.miro.MiroTransformable
import uk.ac.wellcome.models.{CalmTransformable, MergedSierraRecord, SierraBibRecord}
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

  def createValidEmptySierraBibSQSMessage(id: String): SQSMessage = {
    val mergedSierraRecord = MergedSierraRecord(id = id, maybeBibData = None)

    sqsMessage(JsonUtil.toJson(mergedSierraRecord).get)
  }

  def createValidSierraBibSQSMessage(id: String, title: String, lastModifiedDate: Instant): SQSMessage = {
    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title"
         |}
      """.stripMargin

    val mergedSierraRecord = MergedSierraRecord(id = id, maybeBibData = Some(SierraBibRecord(id, data, lastModifiedDate)))

    sqsMessage(JsonUtil.toJson(mergedSierraRecord).get)
  }


  def createValidMiroSQSMessage(data: String): SQSMessage = {
    val miroTransformable = MiroTransformable("id", "collection", data)

    sqsMessage(JsonUtil.toJson(miroTransformable).get)
  }

  def createValidMiroRecord(MiroID: String,
                            MiroCollection: String,
                            data: String): SQSMessage = {
    val miroTransformable = MiroTransformable(MiroID, MiroCollection, data)
    val value = JsonUtil.toJson(miroTransformable).get
    sqsMessage(value)
  }

  def createInvalidRecord: SQSMessage = sqsMessage("not a json string")

  def sqsMessage(message: String) = {
    SQSMessage(None, message, "test_transformer_topic", "notification", "")
  }
}
