package uk.ac.wellcome.platform.archiver

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
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

  it("continues after failure") {
    withArchiver {
      case (ingestBucket, storageBucket, queuePair, archiver) =>
      withFakeBag(ingestBucket, queuePair) { validBag1 =>
        archiver.run()
        withFakeBag(ingestBucket, queuePair, false) { invalidBag1 =>
          withFakeBag(ingestBucket, queuePair) { validBag2 =>
            withFakeBag(ingestBucket, queuePair, false) { invalidBag2 =>

              eventually {
                assertQueuePairSizes(queuePair, 0, 2)
              }
            }
          }
        }
      }
    }
  }

  it("downloads, uploads and verifies a BagIt bag") {
    withArchiver {
      case (ingestBucket, storageBucket, queuePair, archiver) =>
      withFakeBag(ingestBucket, queuePair) { invalidBag =>
        archiver.run()
        eventually {
          listKeysInBucket(storageBucket) should have size 27
          assertQueuePairSizes(queuePair, 0, 0)
        }
      }
    }
  }

  it("fails when ingesting an invalid bag") {
    withArchiver {
      case (ingestBucket, storageBucket, queuePair, archiver) =>
        withFakeBag(ingestBucket, queuePair, false) { invalidBag =>
          archiver.run()
          eventually {

            assertQueuePairSizes(queuePair, 0, 1)

          }
        }
    }
  }

}
