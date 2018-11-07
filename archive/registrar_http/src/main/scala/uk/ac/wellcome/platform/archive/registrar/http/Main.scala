package uk.ac.wellcome.platform.archive.registrar.http

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.config.builders.{DynamoBuilder, HTTPServerBuilder, S3Builder, VHSBuilder}
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config = ConfigFactory.load()

  implicit val storageBackend: S3StorageBackend = new S3StorageBackend(
    s3Client = S3Builder.buildS3Client(config)
  )

  val vhs = new VersionedHybridStore[
    StorageManifest,
    EmptyMetadata,
    ObjectStore[StorageManifest]](
    vhsConfig = VHSBuilder.buildVHSConfig(config),
    objectStore = ObjectStore[StorageManifest],
    dynamoDbClient = DynamoBuilder.buildDynamoClient(config)
  )

  val registrarHTTP = new RegistrarHTTP(
    vhs = vhs,
    httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
    contextURL = HTTPServerBuilder.buildContextURL(config)
  )

  try {
    info(s"Starting service.")

    val app = registrarHTTP.run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating service.")
  }
}
