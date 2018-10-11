package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.File
import java.net.URI
import java.util.zip.ZipFile

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Guice
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContextGenerators
import uk.ac.wellcome.platform.archive.archivist.modules.{
  ConfigModule,
  TestAppConfigModule
}
import uk.ac.wellcome.platform.archive.archivist.{Archivist => ArchivistApp}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait Archivist
    extends Messaging
    with ZipBagItFixture
    with IngestRequestContextGenerators {

  import IngestBagRequest._

  def sendBag[R](
    zipFile: ZipFile,
    ingestBucket: Bucket,
    callbackUri: Option[URI],
    queuePair: QueuePair)(testWith: TestWith[IngestBagRequest, R]) = {

    val ingestBagRequest = createIngestBagRequestWith(
      ingestBagLocation = ObjectLocation(
        ingestBucket.name,
        s"${randomAlphanumeric()}.zip"
      ),
      callbackUri = callbackUri
    )

    val bucket = ingestBagRequest.zippedBagLocation.namespace
    val key = ingestBagRequest.zippedBagLocation.key

    s3Client.putObject(bucket, key, new File(zipFile.getName))

    sendNotificationToSQS(
      queuePair.queue,
      ingestBagRequest
    )

    testWith(ingestBagRequest)
  }

  def createAndSendBag[R](
    ingestBucket: Bucket,
    callbackUri: Option[URI],
    queuePair: QueuePair,
    dataFileCount: Int = 12,
    createDigest: String => String = createValidDigest,
    createDataManifest: List[(String, String)] => Option[FileEntry] =
      createValidDataManifest,
    createBagItFile: => Option[FileEntry] = createValidBagItFile,
    createBagInfoFile: String => Option[FileEntry] = createValidBagInfoFile)(
    testWith: TestWith[(IngestBagRequest, String), R]) =
    withBagItZip(
      dataFileCount = dataFileCount,
      createDigest = createDigest,
      createDataManifest = createDataManifest,
      createBagItFile = createBagItFile,
      createBagInfoFile = createBagInfoFile
    ) {
      case (bagIdentifier, zipFile) =>
        sendBag(zipFile, ingestBucket, callbackUri, queuePair) {
          case ingestBagRequest =>
            testWith((ingestBagRequest, bagIdentifier))
        }
    }

  def withApp[R](actorSystem: ActorSystem,
                 actorMaterializer: ActorMaterializer,
                 storageBucket: Bucket,
                 queuePair: QueuePair,
                 registrarTopic: Topic,
                 progressTopic: Topic)(testWith: TestWith[ArchivistApp, R]) = {
    val archivist = new ArchivistApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          actorSystem,
          actorMaterializer,
          queuePair.queue.url,
          storageBucket.name,
          registrarTopic.arn,
          progressTopic.arn),
        ConfigModule,
        S3ClientModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSClientModule
      )
    }
    testWith(archivist)
  }

  def withArchivist[R](
    testWith: TestWith[(Bucket, Bucket, QueuePair, Topic, Topic, ArchivistApp),
                       R]) = {
    withLocalSqsQueueAndDlqAndTimeout(5)(queuePair => {
      withLocalSnsTopic {
        registrarTopic =>
          withLocalSnsTopic {
            progressTopic =>
              withLocalS3Bucket {
                ingestBucket =>
                  withLocalS3Bucket {
                    storageBucket =>
                      withActorSystem {
                        actorSystem =>
                          withMaterializer(actorSystem) { materializer =>
                            withApp(
                              actorSystem,
                              materializer,
                              storageBucket,
                              queuePair,
                              registrarTopic,
                              progressTopic) { archivist =>
                              testWith(
                                (
                                  ingestBucket,
                                  storageBucket,
                                  queuePair,
                                  registrarTopic,
                                  progressTopic,
                                  archivist))
                            }
                          }
                      }
                  }
              }
          }
      }
    })
  }

}
