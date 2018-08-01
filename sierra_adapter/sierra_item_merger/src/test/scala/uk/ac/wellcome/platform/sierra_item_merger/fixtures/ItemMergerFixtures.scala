package uk.ac.wellcome.platform.sierra_item_merger.fixtures

import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.utils.JsonUtil._

trait ItemMergerFixtures extends IdentifiersUtil with S3 with SQS {
  def createItemNotification(bucket: Bucket, itemRecord: SierraItemRecord): NotificationMessage = {
    val key = s"messaging/${randomAlphanumeric(10)}.json"
    s3Client.putObject(bucket.name, key, toJson(itemRecord).get)

    val hybridRecord = HybridRecord(
      id = itemRecord.id.withoutCheckDigit,
      s3key = key,
      version = 1
    )

    createNotificationMessageWith(hybridRecord)
  }
}
