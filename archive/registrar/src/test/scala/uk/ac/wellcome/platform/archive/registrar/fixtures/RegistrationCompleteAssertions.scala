package uk.ac.wellcome.platform.archive.registrar.fixtures
import org.scalatest.{Inside, Matchers}
import uk.ac.wellcome.platform.archive.common.models.BagLocation
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait RegistrationCompleteAssertions extends Inside with Matchers{
  def assertRegistrationComplete(storageBucket: Bucket, bagLocation: BagLocation, registrationComplete: RegistrationComplete, filesNumber: Long): Unit = {
    inside(registrationComplete.storageManifest) {
      case StorageManifest(
      bagId,
      sourceIdentifier,
      identifiers,
      FileManifest(ChecksumAlgorithm("sha256"), bagDigestFiles),
      TagManifest(ChecksumAlgorithm("sha256"), Nil),
      List(digitalLocation),
      _,
      _,
      _,
      _) =>
        bagId shouldBe BagId(bagLocation.bagPath.value)
        sourceIdentifier shouldBe SourceIdentifier(
          IdentifierType("source", "Label"),
          value = "123"
        )
        identifiers shouldBe List(sourceIdentifier)
        bagDigestFiles should have size filesNumber

        digitalLocation shouldBe DigitalLocation(
          s"http://${storageBucket.name}.s3.amazonaws.com/${bagLocation.storagePath}/${bagLocation.bagPath.value}",
          LocationType(
            "aws-s3-standard-ia",
            "AWS S3 Standard IA"
          )
        )
    }

  }
}
