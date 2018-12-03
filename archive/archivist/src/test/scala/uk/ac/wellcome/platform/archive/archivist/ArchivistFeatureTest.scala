package uk.ac.wellcome.platform.archive.archivist

import java.util.UUID

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.archivist.fixtures.ArchivistFixtures
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.Progress

class ArchivistFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with RandomThings
    with MetricsSenderFixture
    with ArchivistFixtures
    with IntegrationPatience
    with ProgressUpdateAssertions {

  it("downloads, uploads and verifies a BagIt bag") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          registrarTopic,
          progressTopic) =>
        createAndSendBag(ingestBucket, queuePair) {
          case (request, bagIdentifier) =>
            eventually {

              val archivedObjects = listKeysInBucket(storageBucket)
              archivedObjects should have size 16
              val archivedObjectNames = archivedObjects.map(_.split("/").last)

              archivedObjectNames should contain allElementsOf List(
                "bag-info.txt",
                "bagit.txt",
                "manifest-sha256.txt",
                "tagmanifest-sha256.txt")

              assertQueuePairSizes(queuePair, 0, 0)

              assertSnsReceivesOnly(
                ArchiveComplete(
                  request.archiveRequestId,
                  request.storageSpace,
                  BagLocation(
                    storageBucket.name,
                    "archive",
                    BagPath(s"${request.storageSpace}/$bagIdentifier"))
                ),
                registrarTopic
              )

              assertTopicReceivesProgressEventUpdate(
                request.archiveRequestId,
                progressTopic) { events =>
                events should have size 1
                events.head.description shouldBe s"Started working on ingestRequest: ${request.archiveRequestId}"
              }

              assertTopicReceivesProgressEventUpdate(
                request.archiveRequestId,
                progressTopic) { events =>
                events should have size 1
                events.head.description shouldBe "zipFile downloaded successfully"
              }

              assertTopicReceivesProgressEventUpdate(
                request.archiveRequestId,
                progressTopic) { events =>
                events should have size 1
                events.head.description shouldBe "Bag uploaded and verified successfully"
              }

            }
        }
    }
  }

  it("fails when ingesting an invalid bag") {
    withArchivist {
      case (ingestBucket, _, queuePair, registrarTopic, progressTopic) =>
        createAndSendBag(
          ingestBucket,
          queuePair,
          createDigest = _ => "bad_digest") {
          case (request, _) =>
            eventually {
              assertQueuePairSizes(queuePair, 0, 0)
              assertSnsReceivesNothing(registrarTopic)

              assertTopicReceivesProgressStatusUpdate(
                request.archiveRequestId,
                progressTopic,
                Progress.Failed)({ events =>
                all(events.map(_.description)) should include regex "Calculated checksum .+ was different from bad_digest"
              })
            }
        }
    }
  }

  it("fails when ingesting a bag with no tag manifest") {
    withArchivist {
      case (ingestBucket, _, queuePair, registrarTopic, progressTopic) =>
        createAndSendBag(ingestBucket, queuePair, createTagManifest = _ => None) {
          case (request, _) =>
            eventually {
              assertQueuePairSizes(queuePair, 0, 0)
              assertSnsReceivesNothing(registrarTopic)

              assertTopicReceivesProgressStatusUpdate(
                request.archiveRequestId,
                progressTopic,
                Progress.Failed)({ events =>
                all(events.map(_.description)) should include regex "Failed reading file tagmanifest-sha256.txt from zip file"
              })
            }
        }
    }
  }

  it("continues after bag with bad digest") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          registrarTopic,
          progressTopic) =>
        createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
          case (validRequest1, validBag1) =>
            createAndSendBag(
              ingestBucket,
              queuePair,
              dataFileCount = 1,
              createDigest = _ => "bad_digest") {
              case (invalidRequest1, _) =>
                createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
                  case (validRequest2, validBag2) =>
                    createAndSendBag(
                      ingestBucket,
                      queuePair,
                      dataFileCount = 1,
                      createDigest = _ => "bad_digest") {
                      case (invalidRequest2, _) =>
                        eventually {

                          assertQueuePairSizes(queuePair, 0, 0)

                          assertSnsReceives(
                            Set(
                              ArchiveComplete(
                                validRequest1.archiveRequestId,
                                validRequest1.storageSpace,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(
                                    s"${validRequest1.storageSpace}/$validBag1"))
                              ),
                              ArchiveComplete(
                                validRequest2.archiveRequestId,
                                validRequest2.storageSpace,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(
                                    s"${validRequest2.storageSpace}/$validBag2"))
                              )
                            ),
                            registrarTopic
                          )

                          assertTopicReceivesProgressStatusUpdate(
                            invalidRequest1.archiveRequestId,
                            progressTopic,
                            Progress.Failed) { events =>
                            all(events.map(_.description)) should include regex "Calculated checksum .+ was different from bad_digest"
                          }

                          assertTopicReceivesProgressStatusUpdate(
                            invalidRequest2.archiveRequestId,
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

  it("continues after non existing zip file") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          registrarTopic,
          progressTopic) =>
        createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
          case (validRequest1, validBag1) =>
            val invalidRequestId1 = randomUUID
            sendNotificationToSQS(
              queuePair.queue,
              IngestBagRequest(
                archiveRequestId = invalidRequestId1,
                zippedBagLocation =
                  ObjectLocation(ingestBucket.name, "non-existing1.zip"),
                storageSpace = StorageSpace("not_a_real_one")
              )
            )

            createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
              case (validRequest2, validBag2) =>
                val invalidRequestId2 = randomUUID

                sendNotificationToSQS(
                  queuePair.queue,
                  IngestBagRequest(
                    archiveRequestId = invalidRequestId2,
                    zippedBagLocation =
                      ObjectLocation(ingestBucket.name, "non-existing2.zip"),
                    storageSpace = StorageSpace("not_a_real_one")
                  )
                )

                eventually {

                  assertQueuePairSizes(queuePair, 0, 0)

                  assertSnsReceives(
                    Set(
                      ArchiveComplete(
                        validRequest1.archiveRequestId,
                        validRequest1.storageSpace,
                        BagLocation(
                          storageBucket.name,
                          "archive",
                          BagPath(s"${validRequest1.storageSpace}/$validBag1"))
                      ),
                      ArchiveComplete(
                        validRequest2.archiveRequestId,
                        validRequest2.storageSpace,
                        BagLocation(
                          storageBucket.name,
                          "archive",
                          BagPath(s"${validRequest2.storageSpace}/$validBag2"))
                      )
                    ),
                    registrarTopic
                  )

                  assertTopicReceivesFailedProgress(
                    requestId = invalidRequestId1,
                    expectedDescriptionPrefix =
                      s"Failed downloading zipFile ${ingestBucket.name}/non-existing1.zip",
                    progressTopic = progressTopic
                  )

                  assertTopicReceivesFailedProgress(
                    requestId = invalidRequestId2,
                    expectedDescriptionPrefix =
                      s"Failed downloading zipFile ${ingestBucket.name}/non-existing2.zip",
                    progressTopic = progressTopic
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
          registrarTopic,
          progressTopic) =>
        createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
          case (validRequest1, validBag1) =>
            createAndSendBag(
              ingestBucket,
              queuePair,
              dataFileCount = 1,
              createDataManifest = dataManifestWithNonExistingFile) {
              case (invalidRequest1, _) =>
                createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
                  case (validRequest2, validBag2) =>
                    createAndSendBag(
                      ingestBucket,
                      queuePair,
                      dataFileCount = 1,
                      createDataManifest = dataManifestWithNonExistingFile) {
                      case (invalidRequest2, _) =>
                        eventually {

                          assertQueuePairSizes(queuePair, 0, 0)

                          assertSnsReceives(
                            Set(
                              ArchiveComplete(
                                validRequest1.archiveRequestId,
                                validRequest2.storageSpace,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(
                                    s"${validRequest1.storageSpace}/$validBag1"))
                              ),
                              ArchiveComplete(
                                validRequest2.archiveRequestId,
                                validRequest2.storageSpace,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(
                                    s"${validRequest2.storageSpace}/$validBag2"))
                              )
                            ),
                            registrarTopic
                          )

                          assertTopicReceivesFailedProgress(
                            requestId = invalidRequest1.archiveRequestId,
                            expectedDescription =
                              "Failed reading file this/does/not/exists.jpg from zip file",
                            progressTopic = progressTopic
                          )

                          assertTopicReceivesFailedProgress(
                            requestId = invalidRequest2.archiveRequestId,
                            expectedDescription =
                              "Failed reading file this/does/not/exists.jpg from zip file",
                            progressTopic = progressTopic
                          )
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
          progressTopic) =>
        createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
          case (validRequest1, validBag1) =>
            createAndSendBag(
              ingestBucket,
              queuePair,
              dataFileCount = 1,
              createBagInfoFile = _ => None) {
              case (invalidRequest1, _) =>
                createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
                  case (validRequest2, validBag2) =>
                    createAndSendBag(
                      ingestBucket,
                      queuePair,
                      dataFileCount = 1,
                      createBagInfoFile = _ => None) {
                      case (invalidRequest2, _) =>
                        eventually {

                          assertQueuePairSizes(queuePair, 0, 0)

                          assertSnsReceives(
                            Set(
                              ArchiveComplete(
                                validRequest1.archiveRequestId,
                                validRequest2.storageSpace,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(
                                    s"${validRequest1.storageSpace}/$validBag1"))
                              ),
                              ArchiveComplete(
                                validRequest2.archiveRequestId,
                                validRequest2.storageSpace,
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(
                                    s"${validRequest2.storageSpace}/$validBag2"))
                              )
                            ),
                            registrarTopic
                          )

                          assertTopicReceivesFailedProgress(
                            requestId = invalidRequest1.archiveRequestId,
                            expectedDescription =
                              "Failed reading file bag-info.txt from zip file",
                            progressTopic = progressTopic
                          )

                          assertTopicReceivesFailedProgress(
                            requestId = invalidRequest2.archiveRequestId,
                            expectedDescription =
                              "Failed reading file bag-info.txt from zip file",
                            progressTopic = progressTopic
                          )
                        }
                    }
                }
            }
        }
    }
  }

  private def assertTopicReceivesFailedProgress(
    requestId: UUID,
    expectedDescription: String = "",
    expectedDescriptionPrefix: String = "",
    progressTopic: Topic
  ) =
    assertTopicReceivesProgressStatusUpdate(
      requestId = requestId,
      progressTopic = progressTopic,
      status = Progress.Failed,
      expectedBag = None) { events =>
      events should have size 1

      if (!expectedDescription.isEmpty) {
        events.head.description shouldBe expectedDescription
      }

      if (!expectedDescriptionPrefix.isEmpty) {
        events.head.description should startWith(expectedDescriptionPrefix)
      }
    }
}
