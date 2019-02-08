package uk.ac.wellcome.platform.goobi_reader.services

import java.io.{ByteArrayInputStream, InputStream}
import java.time.Instant
import java.time.temporal.ChronoUnit

import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{Assertion, FunSpec, Inside}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.fixtures.{Messaging, SQS}
import uk.ac.wellcome.messaging.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.goobi_reader.fixtures.GoobiReaderFixtures
import uk.ac.wellcome.platform.goobi_reader.models.GoobiRecordMetadata
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.{
  LocalDynamoDb,
  LocalVersionedHybridStore
}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{HybridRecord, VersionedHybridStore}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

class GoobiReaderWorkerServiceTest
    extends FunSpec
    with Akka
    with MetricsSenderFixture
    with ScalaFutures
    with IntegrationPatience
    with MockitoSugar
    with GoobiReaderFixtures
    with Messaging
    with SQS
    with Inside
    with LocalDynamoDb
    with LocalVersionedHybridStore {

  private val id = "mets-0001"
  private val goobiS3Prefix = "goobi"
  private val sourceKey = s"$id.xml"
  private val contents = "muddling the machinations of morose METS"
  private val updatedContents = "Updated contents"
  private val eventTime = Instant.parse("2018-01-01T01:00:00.000Z")

  it("processes a notification") {
    withGoobiReaderWorkerService(s3Client) {
      case (bucket, QueuePair(queue, _), _, table, _) =>
        s3Client.putObject(bucket.name, sourceKey, contents)

        sendNotificationToSQS(
          queue = queue,
          body = anS3Notification(sourceKey, bucket.name, eventTime)
        )

        eventually {
          assertRecordStored(
            id = id,
            version = 1,
            expectedMetadata = GoobiRecordMetadata(eventTime),
            expectedContents = contents,
            table = table,
            bucket = bucket)
        }
    }
  }

  it("ingests an object with a space in the s3Key") {
    withGoobiReaderWorkerService(s3Client) {
      case (bucket, QueuePair(queue, _), _, table, _) =>
        val sourceKey = s"$id work.xml"
        val urlEncodedSourceKey =
          java.net.URLEncoder.encode(sourceKey, "utf-8")
        s3Client.putObject(bucket.name, sourceKey, contents)

        sendNotificationToSQS(
          queue = queue,
          body = anS3Notification(urlEncodedSourceKey, bucket.name, eventTime)
        )

        eventually {

          assertRecordStored(
            id = s"$id work",
            version = 1,
            expectedMetadata = GoobiRecordMetadata(eventTime),
            expectedContents = contents,
            table = table,
            bucket = bucket)
        }
    }
  }

  it("doesn't overwrite a newer work with an older work") {
    withGoobiReaderWorkerService(s3Client) {
      case (bucket, QueuePair(queue, dlq), _, table, vhs) =>
        val contentStream = new ByteArrayInputStream(contents.getBytes)
        vhs.updateRecord(id = id)(
          ifNotExisting = (contentStream, GoobiRecordMetadata(eventTime)))(
          ifExisting = (t, m) => (t, m))
        eventually {
          val expectedMetadata = GoobiRecordMetadata(eventTime)
          assertRecordStored(
            id = id,
            version = 1,
            expectedMetadata = expectedMetadata,
            expectedContents = contents,
            table = table,
            bucket = bucket)
        }

        s3Client.putObject(bucket.name, sourceKey, contents)

        val olderEventTime = eventTime.minus(1, ChronoUnit.HOURS)

        sendNotificationToSQS(
          queue = queue,
          body = anS3Notification(sourceKey, bucket.name, olderEventTime)
        )

        eventually {

          val expectedMetadata = GoobiRecordMetadata(eventTime)
          assertRecordStored(
            id = id,
            version = 1,
            expectedMetadata,
            contents,
            table,
            bucket)
        }
    }
  }

  it("overwrites an older work with an newer work") {
    withGoobiReaderWorkerService(s3Client) {
      case (bucket, QueuePair(queue, dlq), _, table, vhs) =>
        val contentStream = new ByteArrayInputStream(contents.getBytes)
        vhs.updateRecord(id = id)(
          ifNotExisting = (contentStream, GoobiRecordMetadata(eventTime)))(
          ifExisting = (t, m) => (t, m))
        eventually {

          assertRecordStored(
            id = id,
            version = 1,
            GoobiRecordMetadata(eventTime),
            contents,
            table,
            bucket)
        }

        s3Client.putObject(bucket.name, sourceKey, updatedContents)
        val newerEventTime = eventTime.plus(1, ChronoUnit.HOURS)

        sendNotificationToSQS(
          queue = queue,
          body = anS3Notification(sourceKey, bucket.name, newerEventTime)
        )

        eventually {
          val expectedMetadata = GoobiRecordMetadata(newerEventTime)
          assertRecordStored(
            id = id,
            version = 2,
            expectedMetadata,
            updatedContents,
            table,
            bucket)
        }
    }
  }

  it("fails gracefully if Json cannot be parsed") {
    withGoobiReaderWorkerService(s3Client) {
      case (bucket, QueuePair(queue, dlq), metricsSender, table, _) =>
        sendNotificationToSQS(
          queue = queue,
          body = "NotJson"
        )

        eventually {
          assertMessageSentToDlq(queue, dlq)
          assertUpdateNotSaved(bucket, table)
          verify(metricsSender, times(3))
            .countRecognisedFailure(any[String])
        }
    }
  }

  it("does not fail gracefully if content cannot be fetched") {
    withGoobiReaderWorkerService(s3Client) {
      case (bucket, QueuePair(queue, dlq), metricsSender, table, _) =>
        val sourceKey = "NotThere.xml"

        sendNotificationToSQS(
          queue = queue,
          body = anS3Notification(sourceKey, bucket.name, eventTime)
        )

        eventually {
          assertMessageSentToDlq(queue, dlq)
          assertUpdateNotSaved(bucket, table)
          verify(metricsSender, times(3))
            .countFailure(any[String])
        }
    }
  }

  it("does not fail gracefully when there is an unexpected failure") {
    val mockClient = mock[AmazonS3Client]
    val expectedException = new RuntimeException("Failed!")
    when(mockClient.getObject(any[String], any[String]))
      .thenThrow(expectedException)
    withGoobiReaderWorkerService(mockClient) {
      case (bucket, QueuePair(queue, dlq), metricsSender, table, _) =>
        val sourceKey = "any.xml"
        sendNotificationToSQS(
          queue = queue,
          body = anS3Notification(sourceKey, bucket.name, eventTime)
        )

        eventually {
          assertMessageSentToDlq(queue, dlq)
          assertUpdateNotSaved(bucket, table)
          verify(metricsSender, times(3))
            .countFailure(any[String])
        }
    }
  }

  private def assertRecordStored(id: String,
                                 version: Int,
                                 expectedMetadata: GoobiRecordMetadata,
                                 expectedContents: String,
                                 table: Table,
                                 bucket: Bucket): Assertion =
    inside(getHybridRecord(table, id)) {
      case HybridRecord(actualId, actualVersion, location) =>
        actualId shouldBe id
        actualVersion shouldBe version
        getRecordMetadata[GoobiRecordMetadata](table, id) shouldBe expectedMetadata
        getContentFromS3(location) shouldBe expectedContents
    }

  private def assertMessageSentToDlq(queue: Queue, dlq: Queue) = {
    assertQueueEmpty(queue)
    assertQueueHasSize(dlq, 1)
  }

  private def assertUpdateNotSaved(bucket: Bucket, table: Table) = {
    assertDynamoTableIsEmpty(table)
    assertS3StorageIsEmpty(bucket)
  }

  private def assertDynamoTableIsEmpty(table: Table) = {
    Scanamo.scan[HybridRecord](dynamoDbClient)(table.name) shouldBe empty
  }

  private def assertS3StorageIsEmpty(bucket: Bucket) =
    listKeysInBucket(bucket) shouldBe empty

  private def withS3StreamStoreFixtures[R](
    testWith: TestWith[(Bucket,
                        Table,
                        VersionedHybridStore[InputStream,
                                             GoobiRecordMetadata,
                                             ObjectStore[InputStream]]),
                       R]): R =
    withLocalS3Bucket[R] { bucket =>
      withLocalDynamoDbTable[R] { table =>
        withTypeVHS[InputStream, GoobiRecordMetadata, R](
          bucket,
          table,
          goobiS3Prefix) { vhs =>
          testWith((bucket, table, vhs))
        }
      }
    }

  private def withGoobiReaderWorkerService[R](s3Client: AmazonS3)(
    testWith: TestWith[(Bucket,
                        QueuePair,
                        MetricsSender,
                        Table,
                        VersionedHybridStore[InputStream,
                                             GoobiRecordMetadata,
                                             ObjectStore[InputStream]]),
                       R]): R =
    withActorSystem { implicit actorSystem =>
      withLocalSqsQueueAndDlq {
        case queuePair @ QueuePair(queue, dlq) =>
          withMockMetricSender { mockMetricsSender =>
            withSQSStream[NotificationMessage, R](
              queue = queue,
              metricsSender = mockMetricsSender) { sqsStream =>
              withS3StreamStoreFixtures {
                case (bucket, table, versionedHybridStore) =>
                  val service = new GoobiReaderWorkerService(
                    s3Client = s3Client,
                    sqsStream = sqsStream,
                    versionedHybridStore = versionedHybridStore
                  )

                  service.run()

                  testWith(
                    (
                      bucket,
                      queuePair,
                      mockMetricsSender,
                      table,
                      versionedHybridStore))
              }
            }
          }
      }
    }
}
