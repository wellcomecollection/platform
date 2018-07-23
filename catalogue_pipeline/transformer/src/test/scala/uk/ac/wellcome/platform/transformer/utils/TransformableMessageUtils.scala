package uk.ac.wellcome.platform.transformer.utils

import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.{MiroTransformable, SierraTransformable}
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{HybridRecord, SourceMetadata}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

trait TransformableMessageUtils extends SierraUtil with SQS {
  def createValidEmptySierraBibNotificationMessage(
    id: String,
    s3Client: AmazonS3,
    bucket: Bucket
  ): NotificationMessage = {

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData = None,
      itemData = Map[String, SierraItemRecord]()
    )

    hybridRecordNotificationMessage(
      message = JsonUtil.toJson(sierraTransformable).get,
      sourceName = "sierra",
      version = 1,
      s3Client = s3Client,
      bucket = bucket
    )
  }

  def createValidSierraTransformableJsonWith(id: String = createSierraRecordNumberString,
                                             title: String): String = {
    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title",
         | "varFields": []
         |}
      """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData = Some(
        createSierraBibRecordWith(
          id = id,
          data = data
        )
      ),
      itemData = Map[String, SierraItemRecord]()
    )

    JsonUtil.toJson(sierraTransformable).get
  }

  def createValidMiroTransformableJson(MiroID: String,
                                       MiroCollection: String,
                                       data: String): String = {
    val miroTransformable =
      MiroTransformable(
        sourceId = MiroID,
        MiroCollection = MiroCollection,
        data = data
      )

    JsonUtil.toJson(miroTransformable).get
  }

  def hybridRecordNotificationMessage(message: String,
                                      sourceName: String,
                                      version: Int = 1,
                                      s3Client: AmazonS3,
                                      bucket: Bucket) = {

    val key = "testSource/1/testId/dshg548.json"
    s3Client.putObject(bucket.name, key, message)

    val hybridRecord = HybridRecord(
      id = "testId",
      version = version,
      s3key = key
    )

    val sourceMetadata = SourceMetadata(
      sourceName = sourceName
    )

    case class JoinedCaseClass(
      id: String,
      version: Int,
      s3key: String,
      sourceName: String
    )

    val joinedCaseClass = JoinedCaseClass(
      id = hybridRecord.id,
      version = hybridRecord.version,
      s3key = hybridRecord.s3key,
      sourceName = sourceMetadata.sourceName
    )

    createNotificationMessageWith(
      message = joinedCaseClass
    )
  }
}
