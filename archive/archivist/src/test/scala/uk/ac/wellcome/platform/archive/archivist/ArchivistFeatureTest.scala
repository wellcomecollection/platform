package uk.ac.wellcome.platform.archive.archivist

import java.net.URI

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.utils.ExtendedPatience
import uk.ac.wellcome.platform.archive.archivist.fixtures.{Archivist => ArchivistFixture}
import uk.ac.wellcome.platform.archive.common.models.{BagArchiveCompleteNotification, BagLocation}

// TODO: Test file boundaries
// TODO: Test shutdown mid-stream does not succeed

class ArchivistFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ArchivistFixture
    with ExtendedPatience {

  val callbackUrl = new URI("http://localhost/archive/complete")

  it("downloads, uploads and verifies a BagIt bag") {
    withArchivist {
      case (ingestBucket, storageBucket, queuePair, topic, archivist) =>
        sendFakeBag(ingestBucket, Some(callbackUrl), queuePair) { case (requestId, uploadLocation, validBag) =>
          archivist.run()
          eventually {
            listKeysInBucket(storageBucket) should have size 27

            assertQueuePairSizes(queuePair, 0, 0)

            assertSnsReceivesOnly(
              BagArchiveCompleteNotification(
                requestId,
                BagLocation(storageBucket.name, "archive", validBag),
                Some(callbackUrl)
              ),
              topic
            )
          }
        }
    }
  }

  it("fails when ingesting an invalid bag") {
    withArchivist {
      case (ingestBucket, storageBucket, queuePair, topic, archivist) =>
        sendFakeBag(ingestBucket, Some(callbackUrl), queuePair, false) { _ =>
          archivist.run()
          eventually {
            assertQueuePairSizes(queuePair, 0, 1)
            assertSnsReceivesNothing(topic)
          }
        }
    }
  }

  it("continues after failure") {
    withArchivist {
      case (ingestBucket, storageBucket, queuePair, topic, archivist) =>
        sendFakeBag(ingestBucket, Some(callbackUrl), queuePair) { case(requestId1, uploadLocation1, validBag1) =>
          archivist.run()
          sendFakeBag(ingestBucket, Some(callbackUrl), queuePair, false) { _ =>
            sendFakeBag(ingestBucket, Some(callbackUrl), queuePair) { case(requestId2, uploadLocation2, validBag2) =>
              sendFakeBag(ingestBucket, Some(callbackUrl), queuePair, false) { _ =>
                eventually {

                  assertQueuePairSizes(queuePair, 0, 2)

                  assertSnsReceives(
                    Set(
                      BagArchiveCompleteNotification(
                        requestId1,
                        BagLocation(storageBucket.name, "archive", validBag1),
                        Some(callbackUrl)
                      ),
                      BagArchiveCompleteNotification(
                        requestId2,
                        BagLocation(storageBucket.name, "archive", validBag2),
                        Some(callbackUrl)
                      )
                    ),
                    topic
                  )
                }
              }
            }
          }
        }
    }
  }
}
