package uk.ac.wellcome.platform.archive.archivist

import java.util.zip.ZipFile

import org.scalatest.{FunSpec, Ignore, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.archivist.fixtures.{
  Archivist => ArchivistFixture
}
import uk.ac.wellcome.platform.archive.common.models.BagPath

@Ignore
// Useful test to troubleshoot running the archivist using a local bagfile
class TroubleshootArchivistLocalBagFileTest
    extends FunSpec
    with Matchers
    with ArchivistFixture
    with MetricsSenderFixture {

  it("downloads, uploads and verifies a known BagIt bag") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          _,
          progressTable,
          archivist) =>
        sendBag(
          BagPath(randomAlphanumeric()),
          new ZipFile(
            List(
              System.getProperty("user.home"),
              "Desktop",
              "b24923333-b.zip"
            ).mkString("/")),
          ingestBucket,
          None,
          queuePair) { invalidBag =>
          archivist.run()

          while (true) {
            Thread.sleep(10000)
            println(s"Uploaded: ${listKeysInBucket(storageBucket).size}")
          }
        }
    }
  }
}
