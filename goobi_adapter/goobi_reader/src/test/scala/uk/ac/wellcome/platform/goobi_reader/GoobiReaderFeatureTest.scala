package uk.ac.wellcome.platform.goobi_reader

import java.time.Instant

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.goobi_reader.fixtures.GoobiReaderFixtures
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.utils.ExtendedPatience

class GoobiReaderFeatureTest
    extends FunSpec
    with fixtures.Server
    with Eventually
    with Matchers
    with ExtendedPatience
    with GoobiReaderFixtures {
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
            message = anS3Notification(sourceKey, bucket.name, eventTime)
          )

          withServer(goobiReaderLocalFlags(queue, bucket, table)) { _ =>
            eventually {
              val expectedRecord = HybridRecord(
                id = id,
                version = 1,
                s3key = "goobi/10/mets-0001/cd92f8d3"
              )

              val hybridRecord: HybridRecord = getHybridRecord(table, id)
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
