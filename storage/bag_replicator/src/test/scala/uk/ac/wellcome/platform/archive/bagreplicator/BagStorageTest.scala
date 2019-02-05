package uk.ac.wellcome.platform.archive.bagreplicator

import com.amazonaws.services.s3.transfer.model.CopyResult
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.bagreplicator.config.ReplicatorDestinationConfig
import uk.ac.wellcome.platform.archive.bagreplicator.fixtures.BagReplicatorFixtures
import uk.ac.wellcome.platform.archive.bagreplicator.storage.BagStorage
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagLocation,
  ExternalIdentifier
}
import uk.ac.wellcome.test.fixtures.TestWith

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
        ) { bagLocation: BagLocation =>
          withBagStorage { bagStorage =>
            val destinationLocation = ReplicatorDestinationConfig(
              namespace = destinationBucket.name,
              rootPath = randomAlphanumeric()
            )

            val result: Future[List[CopyResult]] =
              bagStorage.duplicateBag(
                sourceBagLocation = bagLocation,
                storageDestination = destinationLocation
              )

            whenReady(result) { _ =>
              verifyBagCopied(
                sourceLocation = bagLocation,
                storageDestination = destinationLocation
              )
            }
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
          ) { bagLocation: BagLocation =>
            withBag(
              storageBucket = sourceBucket,
              bagInfo = randomBagInfo.copy(
                externalIdentifier = ExternalIdentifier("prefix_suffix")
              )
            ) { _ =>
              withBagStorage { bagStorage =>
                val destinationLocation = ReplicatorDestinationConfig(
                  namespace = destinationBucket.name,
                  rootPath = randomAlphanumeric()
                )

                val result: Future[List[CopyResult]] =
                  bagStorage.duplicateBag(
                    sourceBagLocation = bagLocation,
                    storageDestination = destinationLocation
                  )

                whenReady(result) { _ =>
                  verifyBagCopied(
                    sourceLocation = bagLocation,
                    storageDestination = destinationLocation
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  private def withBagStorage[R](testWith: TestWith[BagStorage, R]): R = {
    val bagStorage = new BagStorage(s3Client = s3Client)
    testWith(bagStorage)
  }
}
