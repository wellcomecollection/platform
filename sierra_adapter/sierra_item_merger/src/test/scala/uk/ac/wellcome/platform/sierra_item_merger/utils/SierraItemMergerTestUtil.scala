package uk.ac.wellcome.platform.sierra_item_merger.utils

import io.circe.generic.extras.semiauto.deriveDecoder
import org.scalatest.Suite
import uk.ac.wellcome.models.VersionUpdater
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.sierra_adapter.utils.SierraTestUtils
import uk.ac.wellcome.storage.VersionedHybridStoreLocal
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.JsonUtil._

trait SierraItemMergerTestUtil extends SierraTestUtils with SQSLocal with VersionedHybridStoreLocal {

  this: Suite =>

  val queueUrl = createQueueAndReturnUrl("test_item_merger")

  implicit val sierraTransformableUpdater =
    new VersionUpdater[SierraTransformable] {
      override def updateVersion(sierraTransformable: SierraTransformable,
                                 newVersion: Int): SierraTransformable = {
        sierraTransformable.copy(version = newVersion)
      }
    }

  override lazy val tableName = "sierra-item-merger-feature-test-table"
  override lazy val bucketName = "sierra-item-merger-feature-test-bucket"

  lazy implicit val decoder = deriveDecoder[SierraTransformable]

  private def itemRecordString(id: String,
                               updatedDate: String,
                               bibIds: List[String] = List()) =
    s"""
       |{
       |  "id": "$id",
       |  "updatedDate": "$updatedDate",
       |  "deleted": false,
       |  "bibIds": ${toJson(bibIds).get},
       |  "fixedFields": {
       |    "85": {
       |      "label": "REVISIONS",
       |      "value": "1"
       |    },
       |    "86": {
       |      "label": "AGENCY",
       |      "value": "1"
       |    }
       |  },
       |  "varFields": [
       |    {
       |      "fieldTag": "c",
       |      "marcTag": "949",
       |      "ind1": " ",
       |      "ind2": " ",
       |      "subfields": [
       |        {
       |          "tag": "a",
       |          "content": "5722F"
       |        }
       |      ]
       |    }
       |  ]
       |}""".stripMargin

  protected def sierraItemRecord(
    id: String,
    updatedDate: String,
    bibIds: List[String]
  ): SierraItemRecord =
    SierraItemRecord(
      id = id,
      data = itemRecordString(
        id = id,
        updatedDate = updatedDate,
        bibIds = bibIds
      ),
      bibIds = bibIds,
      modifiedDate = updatedDate
    )

  protected def sendItemRecordToSQS(record: SierraItemRecord) = {
    val messageBody = toJson(record).get

    val message = SQSMessage(
      subject = None,
      body = messageBody,
      topic = "topic",
      messageType = "messageType",
      timestamp = "2001-01-01T01:01:01Z"
    )
    sqsClient.sendMessage(queueUrl, toJson(message).get)
  }
}
