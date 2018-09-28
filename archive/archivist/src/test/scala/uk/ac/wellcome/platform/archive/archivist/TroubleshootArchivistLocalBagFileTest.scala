package uk.ac.wellcome.platform.archive.archivist

import java.util.zip.ZipFile

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.archivist.fixtures.{Archivist => ArchivistFixture}

// Useful test to troubleshoot running the archivist using a local bagfile
class TroubleshootArchivistLocalBagFileTest
    extends FunSpec
    with Matchers
    with ArchivistFixture
    with MetricsSenderFixture {

  ignore("downloads, uploads and verifies a known BagIt bag") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          _,
          progressTable,
          archivist) =>
        sendBag(
          new ZipFile(
            List(
              System.getProperty("user.home"),
              "git/platform",
              "b22454408.zip"
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
