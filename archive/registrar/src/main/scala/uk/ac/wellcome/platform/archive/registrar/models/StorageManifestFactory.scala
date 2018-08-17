package uk.ac.wellcome.platform.archive.registrar.models

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.flows.FileSplitterFlow
import uk.ac.wellcome.platform.archive.common.models.BagLocation
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.ExecutionContext

object StorageManifestFactory extends Logging {
  def create(bagLocation: BagLocation)(implicit s3Client: AmazonS3,
                                       materializer: Materializer,
                                       executionContext: ExecutionContext) = {

    val algorithm = "sha256"

    def createBagItMetaFileLocation(name: String) =
      ObjectLocation(
        bagLocation.storageNamespace,
        List(
          bagLocation.storagePath, 
          bagLocation.bagName.value,
          name
        ).mkString("/")
      )

    def s3LocationToSource(location: ObjectLocation) = {
      debug(s"Attempting to get ${location.namespace}/${location.key}")
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
      digestLines.map {
        case (checksum, path) =>
          BagDigestFile(Checksum(checksum), BagFilePath(path))
      }
    }

    val bagInfoTupleFuture = getTuples("bag-info.txt", ": +")
    val manifestTupleFuture = getTuples(s"manifest-$algorithm.txt", " +")
//    val tagManifestTupleFuture = getTuples(s"tagmanifest-$algorithm.txt", " +")

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
      //tagManifestTuples <- tagManifestTupleFuture
      tagManifest = TagManifest(
        ChecksumAlgorithm(algorithm),
        Nil //createBagDigestFiles(tagManifestTuples).toList
      )
    } yield
      StorageManifest(
        id = BagId(bagLocation.bagName.value),
        source = sourceIdentifier,
        identifiers = List(sourceIdentifier),
        manifest = fileManifest,
        tagManifest = tagManifest,
        locations = List(location)
      )
  }
}
