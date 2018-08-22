package uk.ac.wellcome.platform.transformer.utils

import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord

trait HybridRecordMessageHelper extends IdentifiersUtil with SQS {

  def hybridRecordNotificationMessage(message: String, version: Int = 1, s3Client: AmazonS3, bucket: Bucket): NotificationMessage = {

    val key = s"testSource/1/testId/${randomAlphanumeric(10)}.json"
    s3Client.putObject(bucket.name, key, message)

    val hybridRecord = HybridRecord(
      id = "testId",
      version = version,
      s3key = key
    )

    createNotificationMessageWith(
      message = hybridRecord
    )
  }
}
