package uk.ac.wellcome.platform.archive.archivist

import java.net.URI
import java.util.UUID

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.archivist.fixtures.{
  Archivist => ArchivistFixture
}
import uk.ac.wellcome.platform.archive.archivist.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.storage.ObjectLocation
import IngestBagRequest._

// TODO: Test file boundaries

class ArchivistFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ArchivistFixture
    with IntegrationPatience
    with ProgressUpdateAssertions {

  val callbackUri = new URI("http://localhost/archive/complete")
  it("downloads, uploads and verifies a BagIt bag") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          registrarTopic,
          progressTopic,
          archivist) =>
        createAndSendBag(ingestBucket, Some(callbackUri), queuePair) {
          case (requestId, uploadLocation, bagIdentifier) =>
            archivist.run()
            eventually {
              listKeysInBucket(storageBucket) should have size 15

              assertQueuePairSizes(queuePair, 0, 0)

              assertSnsReceivesOnly(
                ArchiveComplete(
                  requestId,
                  BagLocation(
                    storageBucket.name,
                    "archive",
                    BagPath(s"$DigitisedStorageType/$bagIdentifier")),
                  Some(callbackUri)
                ),
                registrarTopic
              )

              assertTopicReceivesProgressUpdate(
                requestId,
                progressTopic,
                Progress.None) { events =>
                events should have size 1
                events.head.description shouldBe s"Started working on ingestRequest: $requestId"
              }

              assertTopicReceivesProgressUpdate(
                requestId,
                progressTopic,
                Progress.None) { events =>
                events should have size 1
                events.head.description shouldBe "zipFile downloaded successfully"
              }

              assertTopicReceivesProgressUpdate(
                requestId,
                progressTopic,
                Progress.None) { events =>
                events should have size 1
                events.head.description shouldBe "Bag uploaded and verified successfully"
              }
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
          registrarTopic,
          progressTopic,
          archivist) =>
        createAndSendBag(
          ingestBucket,
          Some(callbackUri),
          queuePair,
          createDigest = _ => "bad_digest") {
          case (requestId, uploadLocation, bagIdentifier) =>
            archivist.run()
            eventually {
              assertQueuePairSizes(queuePair, 0, 0)
              assertSnsReceivesNothing(registrarTopic)

              assertTopicReceivesProgressUpdate(
                requestId,
                progressTopic,
                Progress.Failed)({ events =>
                all(events.map(_.description)) should include regex "Calculated checksum .+ was different from bad_digest"
              })
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
          registrarTopic,
          progressTopic,
          archivist) => {

        archivist.run()

        createAndSendBag(
          ingestBucket,
          Some(callbackUri),
          queuePair,
          dataFileCount = 1) {
          case (validRequestId1, _, validBag1) =>
            createAndSendBag(
              ingestBucket,
              Some(callbackUri),
              queuePair,
              dataFileCount = 1,
              createDigest = _ => "bad_digest") {
              case (invalidRequestId1, _, _) =>
                createAndSendBag(
                  ingestBucket,
                  Some(callbackUri),
                  queuePair,
                  dataFileCount = 1) {
                  case (validRequestId2, _, validBag2) =>
                    createAndSendBag(
                      ingestBucket,
                      Some(callbackUri),
                      queuePair,
                      dataFileCount = 1,
                      createDigest = _ => "bad_digest") {
                      case (invalidRequestId2, _, _) =>
                        eventually {

                          assertQueuePairSizes(queuePair, 0, 0)

                          assertSnsReceives(
                            Set(
                              ArchiveComplete(
                                validRequestId1,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(s"$DigitisedStorageType/$validBag1")),
                                Some(callbackUri)
                              ),
                              ArchiveComplete(
                                validRequestId2,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(s"$DigitisedStorageType/$validBag2")),
                                Some(callbackUri)
                              )
                            ),
                            registrarTopic
                          )

                          assertTopicReceivesProgressUpdate(
                            invalidRequestId1,
                            progressTopic,
                            Progress.Failed) { events =>
                            all(events.map(_.description)) should include regex "Calculated checksum .+ was different from bad_digest"
                          }

                          assertTopicReceivesProgressUpdate(
                            invalidRequestId2,
                            progressTopic,
                            Progress.Failed) { events =>
                            all(events.map(_.description)) should include regex "Calculated checksum .+ was different from bad_digest"
                          }

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
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          registrarTopic,
          progressTopic,
          archivist) =>
        archivist.run()

        createAndSendBag(
          ingestBucket,
          Some(callbackUri),
          queuePair,
          dataFileCount = 1) {
          case (validRequestId1, _, validBag1) =>
            val invalidRequestId1 = UUID.randomUUID()
            sendNotificationToSQS(
              queuePair.queue,
              IngestBagRequest(
                invalidRequestId1,
                ObjectLocation(ingestBucket.name, "non-existing1.zip"),
                None))

            createAndSendBag(
              ingestBucket,
              Some(callbackUri),
              queuePair,
              dataFileCount = 1) {
              case (validRequestId2, _, validBag2) =>
                val invalidRequestId2 = UUID.randomUUID()
                sendNotificationToSQS(
                  queuePair.queue,
                  IngestBagRequest(
                    invalidRequestId2,
                    ObjectLocation(ingestBucket.name, "non-existing2.zip"),
                    None))

                eventually {

                  assertQueuePairSizes(queuePair, 0, 0)

                  assertSnsReceives(
                    Set(
                      ArchiveComplete(
                        validRequestId1,
                        BagLocation(
                          storageBucket.name,
                          "archive",
                          BagPath(s"$DigitisedStorageType/$validBag1")),
                        Some(callbackUri)
                      ),
                      ArchiveComplete(
                        validRequestId2,
                        BagLocation(
                          storageBucket.name,
                          "archive",
                          BagPath(s"$DigitisedStorageType/$validBag2")),
                        Some(callbackUri)
                      )
                    ),
                    registrarTopic
                  )

                  assertTopicReceivesProgressUpdate(
                    invalidRequestId1,
                    progressTopic,
                    Progress.Failed) { events =>
                    events should have size 1
                    events.head.description should startWith(
                      s"Failed downloading zipFile ${ingestBucket.name}/non-existing1.zip")
                  }

                  assertTopicReceivesProgressUpdate(
                    invalidRequestId2,
                    progressTopic,
                    Progress.Failed) { events =>
                    events should have size 1
                    events.head.description should startWith(
                      s"Failed downloading zipFile ${ingestBucket.name}/non-existing2.zip")
                  }
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
          registrarTopic,
          progressTopic,
          archivist) => {

        archivist.run()

        createAndSendBag(
          ingestBucket,
          Some(callbackUri),
          queuePair,
          dataFileCount = 1) {
          case (validRequestId1, _, validBag1) =>
            createAndSendBag(
              ingestBucket,
              Some(callbackUri),
              queuePair,
              dataFileCount = 1,
              createDataManifest = dataManifestWithNonExistingFile) {
              case (invalidRequestId1, _, _) =>
                createAndSendBag(
                  ingestBucket,
                  Some(callbackUri),
                  queuePair,
                  dataFileCount = 1) {
                  case (validRequestId2, _, validBag2) =>
                    createAndSendBag(
                      ingestBucket,
                      Some(callbackUri),
                      queuePair,
                      dataFileCount = 1,
                      createDataManifest = dataManifestWithNonExistingFile) {
                      case (invalidRequestId2, _, _) =>
                        eventually {

                          assertQueuePairSizes(queuePair, 0, 0)

                          assertSnsReceives(
                            Set(
                              ArchiveComplete(
                                validRequestId1,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(s"$DigitisedStorageType/$validBag1")),
                                Some(callbackUri)
                              ),
                              ArchiveComplete(
                                validRequestId2,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(s"$DigitisedStorageType/$validBag2")),
                                Some(callbackUri)
                              )
                            ),
                            registrarTopic
                          )

                          assertTopicReceivesProgressUpdate(
                            invalidRequestId1,
                            progressTopic,
                            Progress.Failed) { events =>
                            events should have size 1
                            events.head.description shouldBe "Failed reading file this/does/not/exists.jpg from zip file"
                          }

                          assertTopicReceivesProgressUpdate(
                            invalidRequestId2,
                            progressTopic,
                            Progress.Failed) { events =>
                            events should have size 1
                            events.head.description shouldBe "Failed reading file this/does/not/exists.jpg from zip file"
                          }
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
          registrarTopic,
          progressTopic,
          archivist) =>
        archivist.run()

        createAndSendBag(
          ingestBucket,
          Some(callbackUri),
          queuePair,
          dataFileCount = 1) {
          case (validRequestId1, _, validBag1) =>
            createAndSendBag(
              ingestBucket,
              Some(callbackUri),
              queuePair,
              dataFileCount = 1,
              createBagInfoFile = _ => None) {
              case (invalidRequestId1, _, _) =>
                createAndSendBag(
                  ingestBucket,
                  Some(callbackUri),
                  queuePair,
                  dataFileCount = 1) {
                  case (validRequestId2, _, validBag2) =>
                    createAndSendBag(
                      ingestBucket,
                      Some(callbackUri),
                      queuePair,
                      dataFileCount = 1,
                      createBagInfoFile = _ => None) {
                      case (invalidRequestId2, _, _) =>
                        eventually {

                          assertQueuePairSizes(queuePair, 0, 0)

                          assertSnsReceives(
                            Set(
                              ArchiveComplete(
                                validRequestId1,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(s"$DigitisedStorageType/$validBag1")),
                                Some(callbackUri)
                              ),
                              ArchiveComplete(
                                validRequestId2,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(s"$DigitisedStorageType/$validBag2")),
                                Some(callbackUri)
                              )
                            ),
                            registrarTopic
                          )

                          assertTopicReceivesProgressUpdate(
                            invalidRequestId1,
                            progressTopic,
                            Progress.Failed) { events =>
                            events should have size 1
                            events.head.description shouldBe "Failed reading file bag-info.txt from zip file"
                          }

                          assertTopicReceivesProgressUpdate(
                            invalidRequestId2,
                            progressTopic,
                            Progress.Failed) { events =>
                            events should have size 1
                            events.head.description shouldBe "Failed reading file bag-info.txt from zip file"
                          }
                        }
                    }
                }
            }
        }
    }
  }
}
