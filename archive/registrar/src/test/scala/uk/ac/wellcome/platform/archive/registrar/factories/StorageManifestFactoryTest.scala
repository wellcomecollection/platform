package uk.ac.wellcome.platform.archive.registrar.factories

import org.scalatest.{FunSpec, Inside}
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagPath, DigitisedStorageType}
import uk.ac.wellcome.platform.archive.registrar.fixtures.BagLocationFixtures
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.platform.archive.registrar.models.errors.FileNotFoundError
import uk.ac.wellcome.test.fixtures.Akka

class StorageManifestFactoryTest extends FunSpec with BagLocationFixtures with Inside with Akka {
  implicit val _ = s3Client

  it("returns a right of storage manifest if reading a bag location succeeds") {
    withActorSystem { actorSystem =>
    implicit val ec = actorSystem.dispatcher
      withMaterializer(actorSystem){ implicit materialiser =>
      withLocalS3Bucket { bucket =>
        withBag(bucket) { bagLocation =>
          inside(StorageManifestFactory.create(bagLocation)) {
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
    }
  }

  it("returns a left of registrar error if there are no files at the specified bag location") {
    withActorSystem { actorSystem =>
      implicit val ec = actorSystem.dispatcher
      withMaterializer(actorSystem) { implicit materialiser =>
        withLocalS3Bucket { bucket =>
          val bagLocation = BagLocation(
            bucket.name,
            "archive",
            BagPath(s"$DigitisedStorageType/b1234567"))
          val value = StorageManifestFactory.create(bagLocation)
          value shouldBe Left(FileNotFoundError(bagLocation))
        }
      }
    }
  }
}
