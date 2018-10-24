package uk.ac.wellcome.platform.archive.archivist

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.archivist.fixtures.{Archivist => ArchivistFixture}
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, Resource, ResourceIdentifier}

class ArchivistFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with RandomThings
    with MetricsSenderFixture
    with ArchivistFixture
    with IntegrationPatience
    with ProgressUpdateAssertions {

  import IngestBagRequest._

  it("downloads, uploads and verifies a BagIt bag") {
    withArchivist {
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          registrarTopic,
          progressTopic,
          archivist) =>
        createAndSendBag(ingestBucket, queuePair) {
          case (request, bagIdentifier) =>
            archivist.run()
            eventually {
              listKeysInBucket(storageBucket) should have size 15

              assertQueuePairSizes(queuePair, 0, 0)

              assertSnsReceivesOnly(
                ArchiveComplete(
                  request.archiveRequestId,
                  BagId(request.storageSpace, bagIdentifier),
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

              assertTopicReceivesProgressResourceUpdate(
                request.archiveRequestId,
                Resource(ResourceIdentifier(
                  BagId(request.storageSpace, bagIdentifier).toString)),
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
      case (
          ingestBucket,
          storageBucket,
          queuePair,
          registrarTopic,
          progressTopic,
          archivist) =>
        createAndSendBag(
          ingestBucket,
          queuePair,
          createDigest = _ => "bad_digest") {
          case (request, bagIdentifier) =>
            archivist.run()
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
                                BagId(validRequest1.storageSpace, validBag1),
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(
                                    s"${validRequest1.storageSpace}/$validBag1"))
                              ),
                              ArchiveComplete(
                                validRequest2.archiveRequestId,
                                BagId(validRequest2.storageSpace, validBag2),
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

        createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
          case (validRequest1, validBag1) =>
            val invalidRequestId1 = randomUUID
            sendNotificationToSQS(
              queuePair.queue,
              IngestBagRequest(
                invalidRequestId1,
                ObjectLocation(ingestBucket.name, "non-existing1.zip"),
                None,
                StorageSpace("not_a_real_one")
              )
            )

            createAndSendBag(ingestBucket, queuePair, dataFileCount = 1) {
              case (validRequest2, validBag2) =>
                val invalidRequestId2 = randomUUID

                sendNotificationToSQS(
                  queuePair.queue,
                  IngestBagRequest(
                    invalidRequestId2,
                    ObjectLocation(ingestBucket.name, "non-existing2.zip"),
                    None,
                    StorageSpace("not_a_real_one")
                  )
                )

                eventually {

                  assertQueuePairSizes(queuePair, 0, 0)

                  assertSnsReceives(
                    Set(
                      ArchiveComplete(
                        validRequest1.archiveRequestId,
                        BagId(validRequest1.storageSpace, validBag1),
                        BagLocation(
                          storageBucket.name,
                          "archive",
                          BagPath(s"${validRequest1.storageSpace}/$validBag1"))
                      ),
                      ArchiveComplete(
                        validRequest2.archiveRequestId,
                        BagId(validRequest2.storageSpace, validBag2),
                        BagLocation(
                          storageBucket.name,
                          "archive",
                          BagPath(s"${validRequest2.storageSpace}/$validBag2"))
                      )
                    ),
                    registrarTopic
                  )

                  assertTopicReceivesProgressStatusUpdate(
                    invalidRequestId1,
                    progressTopic,
                    Progress.Failed) { events =>
                    events should have size 1
                    events.head.description should startWith(
                      s"Failed downloading zipFile ${ingestBucket.name}/non-existing1.zip")
                  }

                  assertTopicReceivesProgressStatusUpdate(
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
                                BagId(validRequest2.storageSpace, validBag1),
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(
                                    s"${validRequest1.storageSpace}/$validBag1"))
                              ),
                              ArchiveComplete(
                                validRequest2.archiveRequestId,
                                BagId(validRequest2.storageSpace, validBag2),
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
                            events should have size 1
                            events.head.description shouldBe "Failed reading file this/does/not/exists.jpg from zip file"
                          }

                          assertTopicReceivesProgressStatusUpdate(
                            invalidRequest2.archiveRequestId,
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
                                BagId(validRequest2.storageSpace, validBag1),
                                BagLocation(
                                  storageBucket.name,
                                  "archive",
                                  BagPath(
                                    s"${validRequest1.storageSpace}/$validBag1"))
                              ),
                              ArchiveComplete(
                                validRequest2.archiveRequestId,
                                BagId(validRequest2.storageSpace, validBag2),
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
                            events should have size 1
                            events.head.description shouldBe "Failed reading file bag-info.txt from zip file"
                          }

                          assertTopicReceivesProgressStatusUpdate(
                            invalidRequest2.archiveRequestId,
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
