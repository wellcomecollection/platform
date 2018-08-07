package uk.ac.wellcome.platform.archiver

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.storage.utils.ExtendedPatience

class ArchiverFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with fixtures.Archiver
    with ExtendedPatience {

  it("continues after failure") {
    withArchiver {
      case (ingestBucket, storageBucket, queuePair, archiver) =>
        withFakeBag(ingestBucket, queuePair, false) { invalidBag =>
          archiver.run()
          withFakeBag(ingestBucket, queuePair, true) { validBag =>
            eventually {
              assertQueueHasSize(queuePair.queue, 0)
              assertQueueHasSize(queuePair.dlq, 1)
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
            assertQueueHasSize(queuePair.dlq, 0)
            assertQueueHasSize(queuePair.queue, 0)
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
            assertQueueHasSize(queuePair.dlq, 1)
            assertQueueHasSize(queuePair.queue, 0)
          }
        }
    }
  }
}
