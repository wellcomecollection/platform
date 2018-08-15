package uk.ac.wellcome.platform.archive.registrar

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.{Flow, Framing, Sink, Source, StreamConverters}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNSAsync
import com.google.inject._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.platform.archive.common.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{BagArchiveCompleteNotification, BagLocation}
import uk.ac.wellcome.platform.archive.common.modules.S3ClientConfig
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.s3.{S3ClientFactory, S3StorageBackend}
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VHSConfig, VersionedHybridStore}
import uk.ac.wellcome.storage.{ObjectLocation, ObjectStore}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class RegistrarWorker @Inject()(
                                 snsClient: AmazonSNSAsync,
                                 snsConfig: SNSConfig,
                                 s3ClientConfig: S3ClientConfig,
                                 messageStream: MessageStream[NotificationMessage, Object],
                                 dataStore: VersionedHybridStore[StorageManifest, EmptyMetadata, ObjectStore[StorageManifest]],
                                 actorSystem: ActorSystem
                               ) extends Worker[Done] {

  def run() = {

    implicit val client = snsClient
    implicit val system = actorSystem
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = actorSystem.dispatcher

    implicit val s3Client = S3ClientFactory.create(
      region = s3ClientConfig.region,
      endpoint = s3ClientConfig.endpoint.getOrElse(""),
      accessKey = s3ClientConfig.accessKey.getOrElse(""),
      secretKey = s3ClientConfig.secretKey.getOrElse("")
    )

    Flow[NotificationMessage]
      .log("notification message")
      .map(getBagArchiveCompleteNotification)
      .flatMapConcat(notification =>
        Source.fromFuture(
          StorageManifestFactory.create(notification.bagLocation)
        )
      )
      .map(storageManifest => {
        dataStore.updateRecord(storageManifest.id.value)(
          ifNotExisting = (storageManifest, EmptyMetadata())
        )(
          ifExisting = (_, _) => (storageManifest, EmptyMetadata())
        )

        storageManifest
      })
      .map(BagRegistrationCompleteNotification(_))
      .log("created notification")
      .map(toJson(_))
      .map {
        case Success(json) => json
        case Failure(e) => throw e
      }
      .log("notification serialised")
      .via(SnsPublisher.flow(snsConfig.topicArn))
      .log("published notification")

    val workFlow = Flow[NotificationMessage]
      .log("notification")

    messageStream.run("registrar", workFlow)
  }

  private def getBagArchiveCompleteNotification(message: NotificationMessage) = {
    fromJson[BagArchiveCompleteNotification](message.Message) match {
      case Success(location) => location
      case Failure(e) =>
        throw new RuntimeException(
          s"Failed to get object location from notification: ${e.getMessage}"
        )
    }

  }
}

object FileSplitterFlow {
  val framingDelimiter = Framing.delimiter(
    ByteString("\n"),
    maximumFrameLength = 1024,
    allowTruncation = true
  )

  def apply(delimiter: String) = Flow[ByteString]
    .via(framingDelimiter)
    .map(_.utf8String)
    .filter(_.nonEmpty)
    .map(createTuple(_, delimiter))

  private def createTuple(fileChunk: String, delimiter: String) = {
    val splitChunk = fileChunk.split(delimiter).map(_.trim)

    splitChunk match {
      case Array(key: String, value: String) => (key, value)
      case _ => throw MalformedLineException(
        splitChunk.mkString(delimiter)
      )
    }
  }
}

case class MalformedLineException(line: String)
  extends RuntimeException(
    s"Malformed bag digest line: $line")

object StorageManifestFactory {
  def create(bagLocation: BagLocation)(implicit s3Client: AmazonS3, materializer: Materializer, executionContext: ExecutionContext) = {

    val algorithm = "sha256"

    def createBagItMetaFileLocation(name: String) =
      ObjectLocation(
        bagLocation.storageNamespace,
        List(bagLocation.bagName.value, name)
          .mkString("/")
      )

    def s3LocationToSource(location: ObjectLocation) = {
      val s3Object = s3Client.getObject(location.namespace, location.key)
      StreamConverters.fromInputStream(() => s3Object.getObjectContent)
    }

    def getTuples(name: String, delimiter: String) = {
      val location = createBagItMetaFileLocation(name)

      s3LocationToSource(location)
        .via(FileSplitterFlow(delimiter))
        .runWith(Sink.seq)
    }

    def createBagDigestFiles(digestLines: Seq[(String, String)]) = {
      digestLines.map { case (checksum, path) =>
        BagDigestFile(Checksum(checksum), BagFilePath(path))
      }
    }

    val bagInfoTupleFuture = getTuples("bag-info.txt", ": +")
    val manifestTupleFuture = getTuples(s"manifest-$algorithm.txt", " +")
    val tagManifestTupleFuture = getTuples(s"tagmanifest-$algorithm.txt", " +")

    val sourceIdentifier = SourceIdentifier(
      IdentifierType("source", "Label"),
      value = "123"
    )

    val location = DigitalLocation(
      "http://www.example.com/file",
      LocationType("fake", "Fake digital location")
    )

    for {
      bagInfoTuples <- bagInfoTupleFuture
      manifestTuples <- manifestTupleFuture
      fileManifest = FileManifest(
        ChecksumAlgorithm(algorithm),
        createBagDigestFiles(manifestTuples).toList
      )
      tagManifestTuples <- tagManifestTupleFuture
      tagManifest = TagManifest(
        ChecksumAlgorithm(algorithm),
        createBagDigestFiles(tagManifestTuples).toList
      )
    } yield StorageManifest(
      id = BagId(bagLocation.bagName.value),
      source = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      manifest = fileManifest,
      tagManifest = tagManifest,
      locations = List(location)
    )
  }
}

case class BagRegistrationCompleteNotification(storageManifest: StorageManifest)

object VHSModule extends AbstractModule {
  @Provides
  def providesVHSConfig(hybridStoreConfig: HybridStoreConfig) = {
    VHSConfig(
      dynamoConfig = hybridStoreConfig.dynamoConfig,
      s3Config = hybridStoreConfig.s3Config,
      globalS3Prefix = hybridStoreConfig.s3GlobalPrefix
    )
  }

  @Provides
  def providesS3StorageBackend(actorSystem: ActorSystem, hybridStoreConfig: HybridStoreConfig) = {
    val s3Client = S3ClientFactory.create(
      region = hybridStoreConfig.s3ClientConfig.region,
      endpoint = hybridStoreConfig.s3ClientConfig.endpoint.getOrElse(""),
      accessKey = hybridStoreConfig.s3ClientConfig.accessKey.getOrElse(""),
      secretKey = hybridStoreConfig.s3ClientConfig.secretKey.getOrElse("")
    )
    implicit val executionContext = actorSystem.dispatcher
    new S3StorageBackend(s3Client)(executionContext)
  }

  @Provides
  def providesStorageManifestObjectStore(s3StorageBackend: S3StorageBackend, actorSystem: ActorSystem) = {
    implicit val executionContext = actorSystem.dispatcher
    implicit val storageBackend = s3StorageBackend

    ObjectStore[StorageManifest]
  }
}