package uk.ac.wellcome.platform.goobi_reader

import java.time.Instant

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.platform.goobi_reader.fixtures.GoobiReaderFixtures
import uk.ac.wellcome.storage.vhs.HybridRecord

class GoobiReaderFeatureTest
    extends FunSpec
    with fixtures.Server
    with Eventually
    with Matchers
    with IntegrationPatience
    with GoobiReaderFixtures
    with Inside {
  private val eventTime = Instant.parse("2018-01-01T01:00:00.000Z")

  it("gets an S3 notification and puts the new record in VHS") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withLocalSqsQueue { queue =>
          val id = "mets-0001"
          val sourceKey = s"$id.xml"
          val contents = "muddling the machinations of morose METS"

          s3Client.putObject(bucket.name, sourceKey, contents)

          sendNotificationToSQS(
            queue = queue,
            body = anS3Notification(sourceKey, bucket.name, eventTime)
          )

          withServer(goobiReaderLocalFlags(queue, bucket, table)) { _ =>
            eventually {
              val hybridRecord: HybridRecord = getHybridRecord(table, id)
              inside(hybridRecord) {
                case HybridRecord(actualId, actualVersion, s3Key) =>
                  actualId shouldBe id
                  actualVersion shouldBe 1
                  val s3contents = getContentFromS3(bucket, s3Key)
                  s3contents shouldBe contents
              }
            }
          }
        }
      }
    }
  }
}
