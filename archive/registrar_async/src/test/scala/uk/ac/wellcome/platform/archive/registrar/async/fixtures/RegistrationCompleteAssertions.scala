package uk.ac.wellcome.platform.archive.registrar.async.fixtures
import org.scalatest.{Inside, Matchers}
import uk.ac.wellcome.platform.archive.common.models.{BagId, BagLocation}
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait RegistrationCompleteAssertions extends Inside with Matchers {
  def assertRegistrationComplete(storageBucket: Bucket,
                                 bagLocation: BagLocation,
                                 expectedBagId: BagId,
                                 storageManifest: StorageManifest,
                                 filesNumber: Long): Unit = {
    inside(storageManifest) {

      case StorageManifest(
          actualBagId,
          sourceIdentifier,
          identifiers,
          FileManifest(ChecksumAlgorithm("sha256"), bagDigestFiles),
          TagManifest(ChecksumAlgorithm("sha256"), Nil),
          List(digitalLocation),
          _,
          _,
          _,
          _) =>
        actualBagId shouldBe expectedBagId

        sourceIdentifier shouldBe SourceIdentifier(
          IdentifierType("source", "Label"),
          value = "123"
        )

        identifiers shouldBe List(sourceIdentifier)

        bagDigestFiles should have size filesNumber

        val bucketUri = s"http://${storageBucket.name}.s3.amazonaws.com"

        digitalLocation shouldBe DigitalLocation(
          s"$bucketUri/${bagLocation.storagePath}/${bagLocation.bagPath.value}",
          LocationType(
            "aws-s3-standard-ia",
            "AWS S3 Standard IA"
          )
        )
    }
  }
}
