package uk.ac.wellcome.platform.archiver

import java.io.File

import org.scalatest.{FunSpec, Ignore, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archiver.flow.BagName

@Ignore
// Useful test to troubleshoot running the archiver using a local bagfile
class TroubleshootArchiverLocalBagFileTest
    extends FunSpec
    with Matchers
    with fixtures.Archiver
    with MetricsSenderFixture {

  it("downloads, uploads and verifies a known BagIt bag") {
    withArchiver {
      case (ingestBucket, storageBucket, queuePair, _, archiver) =>
        sendBag(
          BagName(randomAlphanumeric()),
          new File(
            List(
              System.getProperty("user.home"),
              "Desktop",
              "b24923333-b.zip"
            ).mkString("/")),
          ingestBucket,
          queuePair) { invalidBag =>
          archiver.run()

          while (true) {
            Thread.sleep(10000)
            println(s"Uploaded: ${listKeysInBucket(storageBucket).size}")
          }
        }
    }
  }
}
