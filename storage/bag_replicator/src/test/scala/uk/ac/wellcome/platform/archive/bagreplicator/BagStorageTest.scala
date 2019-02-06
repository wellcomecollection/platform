package uk.ac.wellcome.platform.archive.bagreplicator

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.bagreplicator.config.ReplicatorDestinationConfig
import uk.ac.wellcome.platform.archive.bagreplicator.fixtures.BagReplicatorFixtures
import uk.ac.wellcome.platform.archive.bagreplicator.storage.BagStorage
import uk.ac.wellcome.platform.archive.common.generators.BagInfoGenerators
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagLocation,
  ExternalIdentifier
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BagStorageTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with BagInfoGenerators
    with BagReplicatorFixtures {

  val bagStorage = new BagStorage(s3Client = s3Client)

  it("duplicates a bag within the same bucket") {
    withLocalS3Bucket { bucket =>
      withBag(bucket) { srcBagLocation =>
        val destinationConfig = ReplicatorDestinationConfig(
          namespace = bucket.name,
          rootPath = randomAlphanumeric()
        )

        val result: Future[BagLocation] = bagStorage.duplicateBag(
          sourceBagLocation = srcBagLocation,
          storageDestination = destinationConfig
        )

        whenReady(result) { dstBagLocation =>
          verifyBagCopied(
            src = srcBagLocation,
            dst = dstBagLocation
          )
        }
      }
    }
  }

  it("duplicates a bag across different buckets") {
    withLocalS3Bucket { sourceBucket =>
      withLocalS3Bucket { destinationBucket =>
        withBag(sourceBucket) { srcBagLocation =>
          val destinationConfig = ReplicatorDestinationConfig(
            namespace = destinationBucket.name,
            rootPath = randomAlphanumeric()
          )

          val result: Future[BagLocation] =
            bagStorage.duplicateBag(
              sourceBagLocation = srcBagLocation,
              storageDestination = destinationConfig
            )

          whenReady(result) { dstBagLocation =>
            verifyBagCopied(
              src = srcBagLocation,
              dst = dstBagLocation
            )
          }
        }
      }
    }
  }

  describe("when other bags have the same prefix") {
    it("should duplicate a bag into a given location") {
      val bagInfo1 = createBagInfoWith(
        externalIdentifier = ExternalIdentifier("prefix")
      )

      val bagInfo2 = createBagInfoWith(
        externalIdentifier = ExternalIdentifier("prefix_suffix")
      )

      withLocalS3Bucket { sourceBucket =>
        withLocalS3Bucket { destinationBucket =>
          withBag(sourceBucket, bagInfo = bagInfo1) { srcBagLocation =>
            withBag(sourceBucket, bagInfo = bagInfo2) { _ =>
              val destinationConfig = ReplicatorDestinationConfig(
                namespace = destinationBucket.name,
                rootPath = randomAlphanumeric()
              )

              val result: Future[BagLocation] =
                bagStorage.duplicateBag(
                  sourceBagLocation = srcBagLocation,
                  storageDestination = destinationConfig
                )

              whenReady(result) { dstBagLocation =>
                verifyBagCopied(
                  src = srcBagLocation,
                  dst = dstBagLocation
                )
              }
            }
          }
        }
      }
    }
  }
}
