package uk.ac.wellcome.platform.goobi_reader

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Random

class GoobiReaderFeatureTest
    extends FunSpec
    with fixtures.Server
    with LocalVersionedHybridStore
    with SQS
    with Eventually
    with Matchers
    with ExtendedPatience {

  it("gets an S3 notification and puts the new record in VHS") {
    val contents = "muddling the machinations of morose METS"

    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withLocalSqsQueue { queue =>
          val id = "mets-0001"
          val sourceKey = s"$id.xml"

          s3Client.putObject(bucket.name, sourceKey, contents)

          val s3notification = s"""{
            |    "Records": [
            |        {
            |            "eventVersion": "2.0",
            |            "eventSource": "aws:s3",
            |            "awsRegion": "eu-west-1",
            |            "eventTime": "2018-05-18T14:00:34.565Z",
            |            "eventName": "ObjectCreated:Put",
            |            "userIdentity": {
            |                "principalId": "AWS:AIDAJCQOG2NMLPWL7OVGQ"
            |            },
            |            "requestParameters": {
            |                "sourceIPAddress": "195.143.129.132"
            |            },
            |            "responseElements": {
            |                "x-amz-request-id": "8AA72F36945DF84E",
            |                "x-amz-id-2": "0sF4Z82rcjaJ4WIKZ7OGEFgSNUZcSqH7J459r4KYBsZ6OyVhgpcbGNeM+wI6llctdLviN/8tgMo="
            |            },
            |            "s3": {
            |                "s3SchemaVersion": "1.0",
            |                "configurationId": "testevent",
            |                "bucket": {
            |                    "name": "${bucket.name}",
            |                    "ownerIdentity": {
            |                        "principalId": "A2BMUDSS9CMZ3O"
            |                    },
            |                    "arn": "arn:aws:s3:::${bucket.name}"
            |                    },
            |                "object": {
            |                    "key": "${sourceKey}",
            |                    "size": 7193624,
            |                    "eTag": "ce96ad12a0e92e97f9c89948967c62e2",
            |                    "sequencer": "005AFEDC82454E713A"
            |                }
            |            }
            |        }
            |    ]
            |}""".stripMargin

          val notificationMessage = NotificationMessage(
            MessageId = Random.nextString(5),
            TopicArn = queue.arn,
            Subject = "Test notification in GoobiReaderFeatureTest",
            Message = s3notification
          )

          sqsClient.sendMessage(
            queue.url,
            toJson(notificationMessage).get
          )

          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(
            bucket,
            table,
            globalS3Prefix = "goobi")

          val expectedRecord = HybridRecord(
            id = id,
            version = 1,
            s3key = "goobi/10/mets-0001/cd92f8d3"
          )

          withServer(flags) { _ =>
            eventually {
              val hybridRecord: HybridRecord =
                getHybridRecord(bucket, table, id)
              hybridRecord shouldBe expectedRecord

              val s3contents = getContentFromS3(bucket, hybridRecord.s3key)
              s3contents shouldBe contents
            }
          }
        }
      }
    }
  }
}
