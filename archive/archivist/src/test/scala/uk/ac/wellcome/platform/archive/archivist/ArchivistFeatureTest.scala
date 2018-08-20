package uk.ac.wellcome.platform.archive.archivist

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.archivist.fixtures.{
  Archivist => ArchivistFixture
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.models.{
  BagArchiveCompleteNotification,
  BagLocation
}

// TODO: Test file boundaries
// TODO: Test shutdown mid-stream does not succeed

class ArchivistFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ArchivistFixture
    with IntegrationPatience {

  it("downloads, uploads and verifies a BagIt bag") {
    withArchivist {
      case (ingestBucket, storageBucket, queuePair, topic, archivist) =>
        sendFakeBag(ingestBucket, queuePair) { validBag =>
          archivist.run()
          eventually {
            listKeysInBucket(storageBucket) should have size 27

            assertQueuePairSizes(queuePair, 0, 0)

            assertSnsReceivesOnly(
              BagArchiveCompleteNotification(
                BagLocation(storageBucket.name, "archive", validBag)
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
        sendFakeBag(ingestBucket, queuePair, false) { invalidBag =>
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
        sendFakeBag(ingestBucket, queuePair) { validBag1 =>
          archivist.run()
          sendFakeBag(ingestBucket, queuePair, false) { invalidBag1 =>
            sendFakeBag(ingestBucket, queuePair) { validBag2 =>
              sendFakeBag(ingestBucket, queuePair, false) { invalidBag2 =>
                eventually {

                  assertQueuePairSizes(queuePair, 0, 2)

                  assertSnsReceives(
                    Set(
                      BagArchiveCompleteNotification(
                        BagLocation(storageBucket.name, "archive", validBag1)
                      ),
                      BagArchiveCompleteNotification(
                        BagLocation(storageBucket.name, "archive", validBag2)
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
