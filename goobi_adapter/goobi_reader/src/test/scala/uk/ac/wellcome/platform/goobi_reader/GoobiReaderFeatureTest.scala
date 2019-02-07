package uk.ac.wellcome.platform.goobi_reader

import java.io.InputStream
import java.time.Instant

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.platform.goobi_reader.fixtures.GoobiReaderFixtures
import uk.ac.wellcome.platform.goobi_reader.models.GoobiRecordMetadata
import uk.ac.wellcome.platform.goobi_reader.services.GoobiReaderWorkerService
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{
  LocalDynamoDb,
  LocalVersionedHybridStore,
  S3
}
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

class GoobiReaderFeatureTest
    extends FunSpec
    with Akka
    with Eventually
    with Matchers
    with IntegrationPatience
    with GoobiReaderFixtures
    with Inside
    with LocalDynamoDb
    with LocalVersionedHybridStore
    with SQS
    with S3 {
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

          withWorkerService(queue, bucket, table) { _ =>
            eventually {
              val hybridRecord: HybridRecord = getHybridRecord(table, id)
              inside(hybridRecord) {
                case HybridRecord(actualId, actualVersion, location) =>
                  actualId shouldBe id
                  actualVersion shouldBe 1
                  val s3contents = getContentFromS3(location)
                  s3contents shouldBe contents
              }
            }
          }
        }
      }
    }
  }

  private def withWorkerService[R](queue: Queue, bucket: Bucket, table: Table)(
    testWith: TestWith[GoobiReaderWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withSQSStream[NotificationMessage, R](queue) { sqsStream =>
        withTypeVHS[InputStream, GoobiRecordMetadata, R](bucket, table) { vhs =>
          val workerService = new GoobiReaderWorkerService(
            s3Client = s3Client,
            sqsStream = sqsStream,
            versionedHybridStore = vhs
          )

          workerService.run()

          testWith(workerService)
        }
      }
    }
}
