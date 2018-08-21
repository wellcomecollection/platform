package uk.ac.wellcome.platform.transformer.utils

import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.work.test.util.IdentifiersGenerators
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait HybridRecordMessageUtils extends IdentifiersGenerators with SQS {

  def hybridRecordNotificationMessage(message: String,
                                      sourceName: String,
                                      version: Int = 1,
                                      s3Client: AmazonS3,
                                      bucket: Bucket) = {

    val key = s"testSource/1/testId/${randomAlphanumeric(10)}.json"
    s3Client.putObject(bucket.name, key, message)

    createNotificationMessageWith(
      ObjectLocation(namespace = bucket.name, key = key)
    )
  }
}
