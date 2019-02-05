package uk.ac.wellcome.platform.archive.bagreplicator

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.bagreplicator.config.ReplicatorDestinationConfig
import uk.ac.wellcome.platform.archive.bagreplicator.fixtures.BagReplicatorFixtures
import uk.ac.wellcome.platform.archive.bagreplicator.storage.BagStorage
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagItemLocation,
  BagLocation,
  ExternalIdentifier
}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BagStorageTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with RandomThings
    with IntegrationPatience
    with BagReplicatorFixtures {

  val bagStorage = new BagStorage(s3Client = s3Client)

  it("duplicates a bag within the same bucket") {
    withLocalS3Bucket { bucket =>
      withBag(bucket) { bagLocation =>
        val destinationConfig = ReplicatorDestinationConfig(
          namespace = bucket.name,
          rootPath = randomAlphanumeric()
        )

        val result: Future[List[BagItemLocation]] = bagStorage.duplicateBag(
          sourceBagLocation = bagLocation,
          storageDestination = destinationConfig
        )

        whenReady(result) { itemLocations =>
          verifyBagCopied(
            sourceLocation = bagLocation,
            storageDestination = destinationConfig,
            expectedItemLocations = itemLocations
          )
        }
      }
    }
  }

  it("duplicates a bag across different buckets") {
    withLocalS3Bucket { sourceBucket =>
      withLocalS3Bucket { destinationBucket =>
        withBag(sourceBucket) { bagLocation =>
          val destinationConfig = ReplicatorDestinationConfig(
            namespace = destinationBucket.name,
            rootPath = randomAlphanumeric()
          )

          val result: Future[List[BagItemLocation]] =
            bagStorage.duplicateBag(
              sourceBagLocation = bagLocation,
              storageDestination = destinationConfig
            )

          whenReady(result) { itemLocations =>
            verifyBagCopied(
              sourceLocation = bagLocation,
              storageDestination = destinationConfig,
              expectedItemLocations = itemLocations
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
            bucket = sourceBucket,
            bagInfo = randomBagInfo.copy(
              externalIdentifier = ExternalIdentifier("prefix")
            )
          ) { bagLocation: BagLocation =>
            withBag(
              bucket = sourceBucket,
              bagInfo = randomBagInfo.copy(
                externalIdentifier = ExternalIdentifier("prefix_suffix")
              )
            ) { _ =>
              val destinationConfig = ReplicatorDestinationConfig(
                namespace = destinationBucket.name,
                rootPath = randomAlphanumeric()
              )

              val result: Future[List[BagItemLocation]] =
                bagStorage.duplicateBag(
                  sourceBagLocation = bagLocation,
                  storageDestination = destinationConfig
                )

              whenReady(result) { itemLocations =>
                verifyBagCopied(
                  sourceLocation = bagLocation,
                  storageDestination = destinationConfig,
                  expectedItemLocations = itemLocations
                )
              }
            }
          }
        }
      }
    }
  }

  def verifyBagCopied(
    sourceLocation: BagLocation,
    storageDestination: ReplicatorDestinationConfig,
    expectedItemLocations: List[BagItemLocation]
  ): Assertion = {
    val sourceItems = getObjectSummaries(
      namespace = sourceLocation.storageNamespace,
      prefix = sourceLocation.completePath
    )

    val sourceKeyEtags = sourceItems.map { _.getETag }

    val destinationItems = getObjectSummaries(
      namespace = storageDestination.namespace,
      prefix = storageDestination.rootPath
    )

    destinationItems.map { _.getKey } shouldBe expectedItemLocations.map { _.completePath }

    val destinationKeyEtags = destinationItems.map { _.getETag }
    destinationKeyEtags should contain theSameElementsAs sourceKeyEtags
  }

  private def getObjectSummaries(namespace: String, prefix: String): List[S3ObjectSummary] =
    s3Client
      .listObjects(namespace, prefix)
      .getObjectSummaries
      .asScala
      .toList
}
