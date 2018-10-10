package uk.ac.wellcome.platform.archive.registrar.factories

import java.net.URI
import java.util.UUID

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.{FunSpec, Inside}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.models.error.{DownloadError, InvalidBagManifestError}
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, BagLocation, BagPath, DigitisedStorageType}
import uk.ac.wellcome.platform.archive.registrar.fixtures.BagLocationFixtures
import uk.ac.wellcome.platform.archive.registrar.models._

class StorageManifestFactoryTest extends FunSpec with BagLocationFixtures with Inside {
  implicit val _ = s3Client

  private val callbackHost = "localhost"
  private val callbackPort = 8080

  def createCallbackUrl = {
    val requestId = UUID.randomUUID()
    (
      Some(new URI(
        s"http://$callbackHost:$callbackPort/callback/$requestId"
      )),
      requestId)
  }

  it("returns a right of storage manifest if reading a bag location succeeds") {
    val (callbackUrl, requestId) = createCallbackUrl

      withLocalS3Bucket { bucket =>
        withBag(bucket) { bagLocation =>
          val archiveComplete = ArchiveComplete(requestId, bagLocation, callbackUrl)
          inside(StorageManifestFactory.create(archiveComplete)) {
            case Right(
                StorageManifest(
                  bagId,
                  sourceIdentifier,
                  identifiers,
                  FileManifest(ChecksumAlgorithm("sha256"), bagDigestFiles),
                  TagManifest(ChecksumAlgorithm("sha256"), Nil),
                  List(digitalLocation),
                  _,
                  _,
                  _,
                  _)) =>
            bagId shouldBe BagId(bagLocation.bagPath.value)
              sourceIdentifier shouldBe SourceIdentifier(
                IdentifierType("source", "Label"),
                value = "123"
              )
              identifiers shouldBe List(sourceIdentifier)
              bagDigestFiles should have size 1

              digitalLocation shouldBe DigitalLocation(
                s"http://${bucket.name}.s3.amazonaws.com/${bagLocation.storagePath}/${bagLocation.bagPath.value}",
                LocationType(
                  "aws-s3-standard-ia",
                  "AWS S3 Standard IA"
                )
              )
          }
        }
      }
      }


  it("returns a left of registrar error if there are no files at the specified bag location") {
    val (callbackUrl, requestId) = createCallbackUrl

        withLocalS3Bucket { bucket =>
          val bagLocation = BagLocation(
            bucket.name,
            "archive",
            BagPath(s"$DigitisedStorageType/b1234567"))
          val archiveComplete = ArchiveComplete(requestId, bagLocation, callbackUrl)
          val value = StorageManifestFactory.create(archiveComplete)

          inside(value) { case Left(DownloadError(exception, _, actualArchiveComplete)) =>
            actualArchiveComplete shouldBe archiveComplete
            exception shouldBe a[AmazonS3Exception]
            exception.asInstanceOf[AmazonS3Exception].getErrorCode shouldBe "NoSuchKey"
          }
        }
      }


  it("returns a left of registrar error if the bagLocation has an invalid manifestfile") {
    val (callbackUrl, requestId) = createCallbackUrl

    withLocalS3Bucket { bucket =>
      withBag(bucket, createDataManifest = _ => Some(FileEntry("manifest-sha256.txt","bleeergh!"))) { bagLocation =>
        val archiveComplete = ArchiveComplete(requestId, bagLocation, callbackUrl)
        val value = StorageManifestFactory.create(archiveComplete)
        value shouldBe Left(InvalidBagManifestError(archiveComplete, "manifest-sha256.txt"))
      }
    }
  }

}
