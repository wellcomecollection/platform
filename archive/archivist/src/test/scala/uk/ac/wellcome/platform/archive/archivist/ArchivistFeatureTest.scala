package uk.ac.wellcome.platform.archive.archivist

import java.net.URI
import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.archivist.fixtures.{Archivist => ArchivistFixture}
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.test.utils.ExtendedPatience

// TODO: Test file boundaries
// TODO: Test shutdown mid-stream does not succeed

class ArchivistFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressMonitorFixture
    with ArchivistFixture
    with ExtendedPatience {

  val callbackUrl = new URI("http://localhost/archive/complete")

  it("downloads, uploads and verifies a BagIt bag") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          topic,
          archivist) =>
        createAndSendBag(ingestBucket, Some(callbackUrl), queuePair) {
          case (requestId, uploadLocation, bagIdentifier) =>
            archivist.run()
            eventually {
              listKeysInBucket(storageBucket) should have size 15

              assertQueuePairSizes(queuePair, 0, 0)

              assertSnsReceivesOnly(
                ArchiveComplete(
                  requestId,
                  BagLocation(storageBucket.name, "archive", BagPath(s"$DigitisedStorageType/$bagIdentifier")),
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
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          topic,
          archivist) =>
        createAndSendBag(
          ingestBucket,
          Some(callbackUrl),
          queuePair,
          createDigest = _ => "bad_digest") { _ =>
          archivist.run()
          eventually {
            assertQueuePairSizes(queuePair, 0, 1)
            assertSnsReceivesNothing(topic)
          }
        }
    }
  }

  it("continues after bag with bad checksum") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          topic,
          archivist) => {

        archivist.run()

        createAndSendBag(
          ingestBucket,
          Some(callbackUrl),
          queuePair,
          dataFileCount = 1) {
          case (requestId1, _, validBag1) =>
            createAndSendBag(
              ingestBucket,
              Some(callbackUrl),
              queuePair,
              dataFileCount = 1,
              createDigest = _ => "bad_digest") { _ =>
              createAndSendBag(
                ingestBucket,
                Some(callbackUrl),
                queuePair,
                dataFileCount = 1) {
                case (requestId2, _, validBag2) =>
                  createAndSendBag(
                    ingestBucket,
                    Some(callbackUrl),
                    queuePair,
                    dataFileCount = 1,
                    createDigest = _ => "bad_digest") { _ =>
                    eventually {

//                      assertQueuePairSizes(queuePair, 0, 2)

                      assertSnsReceives(
                        Set(
                          ArchiveComplete(
                            requestId1,
                            BagLocation(
                              storageBucket.name,
                              "archive",
                              BagPath(s"$DigitisedStorageType/$validBag1")),
                            Some(callbackUrl)
                          ),
                          ArchiveComplete(
                            requestId2,
                            BagLocation(
                              storageBucket.name,
                              "archive",
                              BagPath(s"$DigitisedStorageType/$validBag2")),
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

  it("continues after non existing zip file") {
    withArchivist {
      case (ingestBucket, storageBucket, queuePair, topic, archivist) =>
        archivist.run()

        createAndSendBag(
          ingestBucket,
          Some(callbackUrl),
          queuePair,
          dataFileCount = 1) {
          case (requestId1, _, validBag1) =>
            sendNotificationToSQS(
              queuePair.queue,
              IngestBagRequest(
                UUID.randomUUID(),
                ObjectLocation(ingestBucket.name, "non-existing1.zip"),
                None))

            createAndSendBag(
              ingestBucket,
              Some(callbackUrl),
              queuePair,
              dataFileCount = 1) {
              case (requestId2, _, validBag2) =>
                sendNotificationToSQS(
                  queuePair.queue,
                  IngestBagRequest(
                    UUID.randomUUID(),
                    ObjectLocation(ingestBucket.name, "non-existing2.zip"),
                    None))

                eventually {

                  assertQueuePairSizes(queuePair, 0, 2)

                  assertSnsReceives(
                    Set(
                      ArchiveComplete(
                        requestId1,
                        BagLocation(storageBucket.name, "archive", BagPath(s"$DigitisedStorageType/$validBag1")),
                        Some(callbackUrl)
                      ),
                      ArchiveComplete(
                        requestId2,
                        BagLocation(storageBucket.name, "archive", BagPath(s"$DigitisedStorageType/$validBag2")),
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

  it("continues after non existing file referenced in manifest") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          topic,
          archivist) => {

        archivist.run()

        createAndSendBag(
          ingestBucket,
          Some(callbackUrl),
          queuePair,
          dataFileCount = 1) {
          case (requestId1, _, validBag1) =>
            createAndSendBag(
              ingestBucket,
              Some(callbackUrl),
              queuePair,
              dataFileCount = 1,
              createDataManifest = dataManifestWithNonExistingFile) { _ =>
              createAndSendBag(
                ingestBucket,
                Some(callbackUrl),
                queuePair,
                dataFileCount = 1) {
                case (requestId2, _, validBag2) =>
                  createAndSendBag(
                    ingestBucket,
                    Some(callbackUrl),
                    queuePair,
                    dataFileCount = 1,
                    createDataManifest = dataManifestWithNonExistingFile) { _ =>
                    eventually {

                      assertQueuePairSizes(queuePair, 0, 2)

                      assertSnsReceives(
                        Set(
                          ArchiveComplete(
                            requestId1,
                            BagLocation(
                              storageBucket.name,
                              "archive",
                              BagPath(s"$DigitisedStorageType/$validBag1")),
                            Some(callbackUrl)
                          ),
                          ArchiveComplete(
                            requestId2,
                            BagLocation(
                              storageBucket.name,
                              "archive",
                              BagPath(s"$DigitisedStorageType/$validBag2")),
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

  it("continues after zip file with no bag-info.txt") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          topic,
          archivist) => {

        archivist.run()

        createAndSendBag(
          ingestBucket,
          Some(callbackUrl),
          queuePair,
          dataFileCount = 1) {
          case (requestId1, _, validBag1) =>
            createAndSendBag(
              ingestBucket,
              Some(callbackUrl),
              queuePair,
              dataFileCount = 1,
              createBagInfoFile = _ => None) { _ =>
              createAndSendBag(
                ingestBucket,
                Some(callbackUrl),
                queuePair,
                dataFileCount = 1) {
                case (requestId2, _, validBag2) =>
                  createAndSendBag(
                    ingestBucket,
                    Some(callbackUrl),
                    queuePair,
                    dataFileCount = 1,
                    createBagInfoFile = _ => None) { _ =>
                    eventually {

                      assertQueuePairSizes(queuePair, 0, 2)

                      assertSnsReceives(
                        Set(
                          ArchiveComplete(
                            requestId1,
                            BagLocation(
                              storageBucket.name,
                              "archive",
                              BagPath(s"$DigitisedStorageType/$validBag1")),
                            Some(callbackUrl)
                          ),
                          ArchiveComplete(
                            requestId2,
                            BagLocation(
                              storageBucket.name,
                              "archive",
                              BagPath(s"$DigitisedStorageType/$validBag2")),
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

}
