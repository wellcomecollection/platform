package uk.ac.wellcome.platform.archiver

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}

class ArchiverFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with fixtures.Archiver {

  it("continues after failure") {
    withArchiver {
      case (ingestBucket, storageBucket, queuePair, archiver) =>
        withBag(ingestBucket, queuePair, false) { invalidBag =>
          withBag(ingestBucket, queuePair, true) { validBag =>
            archiver.run()

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
        withBag(ingestBucket, queuePair, false) { invalidBag =>
          archiver.run()

          eventually {
            assertQueueHasSize(queuePair.queue, 0)
            assertQueueHasSize(queuePair.dlq, 0)
          }
        }
    }
  }
}
