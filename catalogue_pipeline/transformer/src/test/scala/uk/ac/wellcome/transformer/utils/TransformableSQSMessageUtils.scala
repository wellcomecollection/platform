package uk.ac.wellcome.transformer.utils

import java.time.Instant

import org.scalatest.Suite
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  MiroTransformable,
  SierraTransformable,
  Transformable
}
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.test.utils.S3Local
import uk.ac.wellcome.utils.JsonUtil

trait TransformableSQSMessageUtils extends S3Local { this: Suite =>

  def createValidCalmTramsformableJson(RecordID: String,
                                       RecordType: String,
                                       AltRefNo: String,
                                       RefNo: String,
                                       data: String): String = {
    val calmTransformable: Transformable =
      CalmTransformable(RecordID, RecordType, AltRefNo, RefNo, data)

    JsonUtil.toJson(calmTransformable).get
  }

  def createValidEmptySierraBibSQSMessage(id: String): SQSMessage = {
    val sierraTransformable: Transformable = SierraTransformable(
      sourceId = id,
      maybeBibData = None,
      itemData = Map[String, SierraItemRecord]()
    )

    hybridRecordSqsMessage(JsonUtil.toJson(sierraTransformable).get)
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

    val sierraTransformable: Transformable = SierraTransformable(
      sourceId = id,
      maybeBibData = Some(SierraBibRecord(id, data, lastModifiedDate)),
      itemData = Map[String, SierraItemRecord]()
    )

    JsonUtil.toJson(sierraTransformable).get
  }

  def createValidMiroTransformableJson(MiroID: String,
                                       MiroCollection: String,
                                       data: String): String = {
    val miroTransformable: Transformable =
      MiroTransformable(MiroID, MiroCollection, data)
    JsonUtil.toJson(miroTransformable).get
  }

  def hybridRecordSqsMessage(message: String) = {
    val key = "testSource/1/testId/dshg548.json"
    s3Client.putObject(bucketName, key, message)

    SQSMessage(
      None,
      JsonUtil
        .toJson(
          HybridRecord(version = 1,
                       sourceId = "testId",
                       sourceName = "testSource",
                       s3key = key))
        .get,
      "test_transformer_topic",
      "notification",
      "the_time"
    )
  }
}
