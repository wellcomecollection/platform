package uk.ac.wellcome.platform.archiver

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archiver.flow.{
  BagArchiveCompleteNotification,
  BagLocation
}
import uk.ac.wellcome.storage.utils.ExtendedPatience

// TODO: Test file boundaries
// TODO: Test shutdown mid-stream does not succeed

class ArchiverFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with fixtures.Archiver
    with MetricsSenderFixture
    with ExtendedPatience {

  it("downloads, uploads and verifies a BagIt bag") {
    withArchiver {
      case (ingestBucket, storageBucket, queuePair, topic, archiver) =>
        sendFakeBag(ingestBucket, queuePair) { validBag =>
          archiver.run()
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
    withArchiver {
      case (ingestBucket, storageBucket, queuePair, topic, archiver) =>
        sendFakeBag(ingestBucket, queuePair, false) { invalidBag =>
          archiver.run()
          eventually {

            assertQueuePairSizes(queuePair, 0, 1)
            assertSnsReceivesNothing(topic)
          }
        }
    }
  }

  it("continues after failure") {
    withArchiver {
      case (ingestBucket, storageBucket, queuePair, topic, archiver) =>
        sendFakeBag(ingestBucket, queuePair) { validBag1 =>
          archiver.run()
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
