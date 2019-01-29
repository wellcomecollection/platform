package uk.ac.wellcome.platform.archive.bagreplicator

import com.amazonaws.services.s3.transfer.model.CopyResult
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.bagreplicator.fixtures.BagReplicatorFixtures
import uk.ac.wellcome.platform.archive.bagreplicator.models.StorageLocation
import uk.ac.wellcome.platform.archive.bagreplicator.storage.{
  BagStorage,
  S3Copier
}
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{
  BagLocation,
  ExternalIdentifier
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BagStorageTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with RandomThings
    with IntegrationPatience
    with BagReplicatorFixtures {

  it("should duplicate a bag into a given location") {

    withLocalS3Bucket { sourceBucket =>
      withLocalS3Bucket { destinationBucket =>
        withBag(
          storageBucket = sourceBucket,
          bagInfo = randomBagInfo
        ) { bagSource: BagLocation =>
          implicit val _s3Client = s3Client
          implicit val _s3Copier = S3Copier()

          val destinationLocation = StorageLocation(
            namespace = destinationBucket.name,
            rootPath = randomAlphanumeric()
          )

          val result: Future[List[CopyResult]] =
            BagStorage.duplicateBag(
              bagSource,
              destinationLocation
            )

          whenReady(result) { _ =>
            verifyBagCopied(
              bagSource,
              destinationLocation
            )
          }
        }
      }
    }
  }

  describe("when other bags have the same prefix") {
    it("should duplicate a bag into a given location") {

      withLocalS3Bucket { sourceBucket =>
        withLocalS3Bucket { destinationBucket =>
          withBag(
            storageBucket = sourceBucket,
            bagInfo = randomBagInfo.copy(
              externalIdentifier = ExternalIdentifier("prefix")
            )
          ) { bagSource: BagLocation =>
            withBag(
              storageBucket = sourceBucket,
              bagInfo = randomBagInfo.copy(
                externalIdentifier = ExternalIdentifier("prefix_suffix")
              )
            ) { _ =>
              implicit val _s3Client = s3Client
              implicit val _s3Copier = S3Copier()

              val destinationLocation = StorageLocation(
                namespace = destinationBucket.name,
                rootPath = randomAlphanumeric()
              )

              val result: Future[List[CopyResult]] =
                BagStorage.duplicateBag(
                  bagSource,
                  destinationLocation
                )

              whenReady(result) { _ =>
                verifyBagCopied(
                  bagSource,
                  destinationLocation
                )
              }
            }
          }
        }
      }
    }
  }
}
