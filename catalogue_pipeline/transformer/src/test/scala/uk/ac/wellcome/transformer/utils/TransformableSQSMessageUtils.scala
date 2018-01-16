package uk.ac.wellcome.transformer.utils

import java.time.Instant

import uk.ac.wellcome.circe.jsonUtil
import uk.ac.wellcome.circe.jsonUtil._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.{CalmTransformable, MiroTransformable, SierraTransformable}
import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraItemRecord}

trait TransformableSQSMessageUtils {

  def createValidCalmSQSMessage(RecordID: String,
                                RecordType: String,
                                AltRefNo: String,
                                RefNo: String,
                                data: String): SQSMessage = {
    val calmTransformable =
      CalmTransformable(RecordID, RecordType, AltRefNo, RefNo, data)

    sqsMessage(jsonUtil.toJson(calmTransformable).get)
  }

  def createValidEmptySierraBibSQSMessage(id: String): SQSMessage = {
    val sierraTransformable = SierraTransformable(
      id = id,
      maybeBibData = None,
      itemData = Map[String, SierraItemRecord]()
    )

    sqsMessage(jsonUtil.toJson(sierraTransformable).get)
  }

  def createValidSierraBibSQSMessage(id: String,
                                     title: String,
                                     lastModifiedDate: Instant): SQSMessage = {
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

    sqsMessage(jsonUtil.toJson(sierraTransformable).get)
  }

  def createValidMiroSQSMessage(data: String): SQSMessage = {
    val miroTransformable = MiroTransformable("id", "collection", data)

    sqsMessage(jsonUtil.toJson(miroTransformable).get)
  }

  def createValidMiroRecord(MiroID: String,
                            MiroCollection: String,
                            data: String): SQSMessage = {
    val miroTransformable = MiroTransformable(MiroID, MiroCollection, data)
    val value = jsonUtil.toJson(miroTransformable).get
    sqsMessage(value)
  }

  def createInvalidRecord: SQSMessage = sqsMessage("not a json string")

  def sqsMessage(message: String) = {
    SQSMessage(None,
               message,
               "test_transformer_topic",
               "notification",
               "the_time")
  }
}
