package uk.ac.wellcome.transformer.utils

import java.time.Instant

import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  MiroTransformable,
  SierraTransformable
}
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.utils.JsonUtil

trait TransformableSQSMessageUtils {

  def createValidCalmTramsformableJson(RecordID: String,
                                       RecordType: String,
                                       AltRefNo: String,
                                       RefNo: String,
                                       data: String): String = {
    val calmTransformable =
      CalmTransformable(RecordID, RecordType, AltRefNo, RefNo, data)

    JsonUtil.toJson(calmTransformable).get
  }

  def createValidEmptySierraBibSQSMessage(id: String): SQSMessage = {
    val sierraTransformable = SierraTransformable(
      id = id,
      maybeBibData = None,
      itemData = Map[String, SierraItemRecord]()
    )

    sqsMessage(JsonUtil.toJson(sierraTransformable).get)
  }

  def createValidSierraTransformableJson(id: String,
                                         title: String,
                                         lastModifiedDate: Instant): String = {
    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title",
         | "varFields": []
         |}
      """.stripMargin

    val sierraTransformable = SierraTransformable(
      id = id,
      maybeBibData = Some(SierraBibRecord(id, data, lastModifiedDate)),
      itemData = Map[String, SierraItemRecord]()
    )

    JsonUtil.toJson(sierraTransformable).get
  }

  def createValidMiroTransformableJson(data: String): String = {
    val miroTransformable = MiroTransformable("id", "collection", data)

    JsonUtil.toJson(miroTransformable).get
  }

  def createValidMiroTransformableJson(MiroID: String,
                                       MiroCollection: String,
                                       data: String): String = {
    val miroTransformable = MiroTransformable(MiroID, MiroCollection, data)
    JsonUtil.toJson(miroTransformable).get
  }

  def createInvalidJson: String = "not a json string"

  def sqsMessage(message: String) = {
    SQSMessage(None,
               message,
               "test_transformer_topic",
               "notification",
               "the_time")
  }
}
