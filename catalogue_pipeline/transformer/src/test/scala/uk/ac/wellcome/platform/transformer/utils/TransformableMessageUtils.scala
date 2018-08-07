package uk.ac.wellcome.platform.transformer.utils

import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{HybridRecord, SourceMetadata}

trait TransformableMessageUtils extends IdentifiersUtil with SQS {
  def createValidMiroTransformableJson(MiroID: String,
                                       MiroCollection: String,
                                       data: String): String = {
    val miroTransformable =
      MiroTransformable(
        sourceId = MiroID,
        MiroCollection = MiroCollection,
        data = data
      )

    toJson(miroTransformable).get
  }

  def hybridRecordNotificationMessage(message: String,
                                      sourceName: String,
                                      version: Int = 1,
                                      s3Client: AmazonS3,
                                      bucket: Bucket) = {

    val key = s"testSource/1/testId/${randomAlphanumeric(10)}.json"
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
