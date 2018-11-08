package uk.ac.wellcome.platform.archive.registrar.async

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.storage.builders.{DynamoBuilder, S3Builder, VHSBuilder}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.config.builders._
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val messageStream =
    MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config)

  implicit val storageBackend: S3StorageBackend = new S3StorageBackend(
    s3Client = S3Builder.buildS3Client(config)
  )

  val dataStore = new VersionedHybridStore[
    StorageManifest,
    EmptyMetadata,
    ObjectStore[StorageManifest]](
    vhsConfig = VHSBuilder.buildVHSConfig(config),
    objectStore = ObjectStore[StorageManifest],
    dynamoDbClient = DynamoBuilder.buildDynamoClient(config)
  )

  val registrar = new Registrar(
    snsClient = SNSBuilder.buildSNSClient(config),
    progressSnsConfig = SNSBuilder.buildSNSConfig(config),
    s3Client = S3Builder.buildS3Client(config),
    messageStream = messageStream,
    dataStore = dataStore,
    actorSystem = actorSystem
  )

  try {
    info(s"Starting worker.")

    val result = registrar.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
