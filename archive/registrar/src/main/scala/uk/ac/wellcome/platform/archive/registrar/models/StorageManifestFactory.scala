package uk.ac.wellcome.platform.archive.registrar.models

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.BagLocation
import uk.ac.wellcome.platform.archive.registrar.flows.FileSplitterFlow
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.ExecutionContext

object StorageManifestFactory extends Logging {
  def create(bagLocation: BagLocation)(implicit s3Client: AmazonS3,
                                       materializer: Materializer,
                                       executionContext: ExecutionContext) = {

    val algorithm = "sha256"

    val manifestTupleFuture = getTuples(bagLocation, s"manifest-$algorithm.txt", " +")
//    val tagManifestTupleFuture = getTuples(s"tagmanifest-$algorithm.txt", " +")

    val sourceIdentifier = SourceIdentifier(
      IdentifierType("source", "Label"),
      value = "123"
    )

    val location = DigitalLocation(
      List(
        s"http://${bagLocation.storageNamespace}.s3.amazonaws.com",
        bagLocation.storagePath,
        bagLocation.bagPath
      ).mkString("/"),
      LocationType(
        "aws-s3-standard-ia",
        "AWS S3 Standard IA"
      )
    )

    for {
      manifestTuples <- manifestTupleFuture
      fileManifest = FileManifest(
        ChecksumAlgorithm(algorithm),
        createBagDigestFiles(manifestTuples).toList
      )
      //tagManifestTuples <- tagManifestTupleFuture
      tagManifest = TagManifest(
        ChecksumAlgorithm(algorithm),
        Nil //createBagDigestFiles(tagManifestTuples).toList
      )
    } yield
      StorageManifest(
        id = BagId(bagLocation.bagPath.value),
        source = sourceIdentifier,
        identifiers = List(sourceIdentifier),
        manifest = fileManifest,
        tagManifest = tagManifest,
        locations = List(location)
      )
  }

  def getTuples(bagLocation: BagLocation, name: String, delimiter: String)(implicit s3Client: AmazonS3, materializer: Materializer) = {
    val location = createBagItMetaFileLocation(bagLocation,name)

    s3LocationToSource(location)
      .via(FileSplitterFlow(delimiter))
      .runWith(Sink.seq)

  }

  def createBagDigestFiles(digestLines: Seq[(String, String)]) = {
    digestLines.map {
      case (checksum, path) =>
        BagDigestFile(Checksum(checksum), BagFilePath(path))
    }
  }

  def createBagItMetaFileLocation(bagLocation: BagLocation,name: String) =
    ObjectLocation(
      bagLocation.storageNamespace,
      List(
        bagLocation.storagePath,
        bagLocation.bagPath.value,
        name
      ).mkString("/")
    )

  def s3LocationToSource(location: ObjectLocation)(implicit s3Client: AmazonS3) = {
    debug(s"Attempting to get ${location.namespace}/${location.key}")
    val s3Object = s3Client.getObject(location.namespace, location.key)
    StreamConverters.fromInputStream(() => s3Object.getObjectContent)
  }
}
