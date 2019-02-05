package uk.ac.wellcome.platform.archive.archivist

import java.util.UUID

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.archivist.fixtures.ArchivistFixtures
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagLocation,
  BagPath
}
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
    withArchivist() {
      case (ingestBucket, storageBucket, queuePair, nextTopic, progressTopic) =>
        val bagInfo = randomBagInfo
        createAndSendBag(ingestBucket, queuePair, bagInfo = bagInfo) {
          request =>
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
                ReplicationRequest(
                  archiveRequestId = request.id,
                  srcBagLocation = BagLocation(
                    storageNamespace = storageBucket.name,
                    storagePrefix = "archive",
                    storageSpace = request.storageSpace,
                    bagPath = BagPath(bagInfo.externalIdentifier.toString)
                  )
                ),
                nextTopic
              )

              assertTopicReceivesProgressEventUpdate(request.id, progressTopic) {
                events =>
                  events should have size 1
                  events.head.description shouldBe "Started work on ingest"
              }

              assertTopicReceivesProgressEventUpdate(request.id, progressTopic) {
                events =>
                  events should have size 1
                  events.head.description shouldBe "Ingest bag file downloaded successfully"
              }

              assertTopicReceivesProgressEventUpdate(request.id, progressTopic) {
                events =>
                  events should have size 1
                  events.head.description shouldBe "Bag uploaded and verified successfully"
              }

            }
        }
    }
  }

  it("fails when ingesting an invalid bag") {
    withArchivist() {
      case (ingestBucket, _, queuePair, nextTopic, progressTopic) =>
        createAndSendBag(
          ingestBucket,
          queuePair,
          createDigest = _ => "bad_digest") { request =>
          eventually {
            assertQueuePairSizes(queuePair, 0, 0)
            assertSnsReceivesNothing(nextTopic)

            assertTopicReceivesProgressStatusUpdate(
              request.id,
              progressTopic,
              Progress.Failed)({ events =>
              all(events.map(_.description)) should include regex "Calculated checksum .+ was different from bad_digest"
            })
          }
        }
    }
  }

  it("fails when ingesting a bag with no tag manifest") {
    withArchivist() {
      case (ingestBucket, _, queuePair, nextTopic, progressTopic) =>
        createAndSendBag(ingestBucket, queuePair, createTagManifest = _ => None) {
          request =>
            eventually {
              assertQueuePairSizes(queuePair, 0, 0)
              assertSnsReceivesNothing(nextTopic)

              assertTopicReceivesProgressStatusUpdate(
                request.id,
                progressTopic,
                Progress.Failed)({ events =>
                all(events.map(_.description)) should include regex "Failed reading file tagmanifest-sha256.txt from zip file"
              })
            }
        }
    }
  }

  it("continues after bag with bad digest") {
    val bagInfo1 = randomBagInfo
    val bagInfo2 = randomBagInfo

    // Parallelism here is 1 as fake-sns can't deal with
    // concurrent requests
    withArchivist(1) {
      case (ingestBucket, storageBucket, queuePair, nextTopic, progressTopic) =>
        createAndSendBag(
          ingestBucket,
          queuePair,
          bagInfo = bagInfo1,
          dataFileCount = 1) { validRequest1 =>
          createAndSendBag(
            ingestBucket,
            queuePair,
            dataFileCount = 1,
            createDigest = _ => "bad_digest") { invalidRequest1 =>
            createAndSendBag(
              ingestBucket,
              queuePair,
              bagInfo = bagInfo2,
              dataFileCount = 1) { validRequest2 =>
              createAndSendBag(
                ingestBucket,
                queuePair,
                dataFileCount = 1,
                createDigest = _ => "bad_digest") { invalidRequest2 =>
                eventually {

                  assertQueuePairSizes(queuePair, 0, 0)

                  assertSnsReceives(
                    Set(
                      ReplicationRequest(
                        archiveRequestId = validRequest1.id,
                        srcBagLocation = BagLocation(
                          storageNamespace = storageBucket.name,
                          storagePrefix = "archive",
                          storageSpace = validRequest1.storageSpace,
                          bagPath =
                            BagPath(bagInfo1.externalIdentifier.toString)
                        )
                      ),
                      ReplicationRequest(
                        archiveRequestId = validRequest2.id,
                        srcBagLocation = BagLocation(
                          storageNamespace = storageBucket.name,
                          storagePrefix = "archive",
                          storageSpace = validRequest2.storageSpace,
                          bagPath =
                            BagPath(bagInfo2.externalIdentifier.toString)
                        )
                      )
                    ),
                    nextTopic
                  )

                  assertTopicReceivesProgressStatusUpdate(
                    invalidRequest1.id,
                    progressTopic,
                    Progress.Failed) { events =>
                    all(events.map(_.description)) should include regex "Calculated checksum .+ was different from bad_digest"
                  }

                  assertTopicReceivesProgressStatusUpdate(
                    invalidRequest2.id,
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
    val bagInfo1 = randomBagInfo
    val bagInfo2 = randomBagInfo

    withArchivist() {
      case (ingestBucket, storageBucket, queuePair, nextTopic, progressTopic) =>
        createAndSendBag(
          ingestBucket,
          queuePair,
          bagInfo = bagInfo1,
          dataFileCount = 1) { validRequest1 =>
          val invalidRequestId1 = randomUUID
          sendNotificationToSQS(
            queuePair.queue,
            IngestBagRequest(
              id = invalidRequestId1,
              zippedBagLocation =
                ObjectLocation(ingestBucket.name, "non-existing1.zip"),
              storageSpace = StorageSpace("not_a_real_one")
            )
          )

          createAndSendBag(
            ingestBucket,
            queuePair,
            bagInfo = bagInfo2,
            dataFileCount = 1) { validRequest2 =>
            val invalidRequestId2 = randomUUID

            sendNotificationToSQS(
              queuePair.queue,
              IngestBagRequest(
                id = invalidRequestId2,
                zippedBagLocation =
                  ObjectLocation(ingestBucket.name, "non-existing2.zip"),
                storageSpace = StorageSpace("not_a_real_one")
              )
            )

            eventually {

              assertQueuePairSizes(queuePair, 0, 0)

              assertSnsReceives(
                Set(
                  ReplicationRequest(
                    archiveRequestId = validRequest1.id,
                    srcBagLocation = BagLocation(
                      storageNamespace = storageBucket.name,
                      storagePrefix = "archive",
                      storageSpace = validRequest1.storageSpace,
                      bagPath = BagPath(bagInfo1.externalIdentifier.toString)
                    )
                  ),
                  ReplicationRequest(
                    archiveRequestId = validRequest2.id,
                    srcBagLocation = BagLocation(
                      storageNamespace = storageBucket.name,
                      storagePrefix = "archive",
                      storageSpace = validRequest2.storageSpace,
                      bagPath = BagPath(bagInfo2.externalIdentifier.toString)
                    )
                  )
                ),
                nextTopic
              )

              assertTopicReceivesFailedProgress(
                requestId = invalidRequestId1,
                expectedDescriptionPrefix =
                  s"Failed downloading file ${ingestBucket.name}/non-existing1.zip",
                progressTopic = progressTopic
              )

              assertTopicReceivesFailedProgress(
                requestId = invalidRequestId2,
                expectedDescriptionPrefix =
                  s"Failed downloading file ${ingestBucket.name}/non-existing2.zip",
                progressTopic = progressTopic
              )
            }
          }
        }

    }
  }

  it("continues after non existing file referenced in manifest") {
    val bagInfo1 = randomBagInfo
    val bagInfo2 = randomBagInfo

    // Parallelism here is 1 as fake-sns can't deal with
    // concurrent requests
    withArchivist(1) {
      case (ingestBucket, storageBucket, queuePair, nextTopic, progressTopic) =>
        createAndSendBag(
          ingestBucket,
          queuePair,
          bagInfo = bagInfo1,
          dataFileCount = 1) { validRequest1 =>
          createAndSendBag(
            ingestBucket,
            queuePair,
            dataFileCount = 1,
            createDataManifest = dataManifestWithNonExistingFile) {
            invalidRequest1 =>
              createAndSendBag(
                ingestBucket,
                queuePair,
                bagInfo = bagInfo2,
                dataFileCount = 1) { validRequest2 =>
                createAndSendBag(
                  ingestBucket,
                  queuePair,
                  dataFileCount = 1,
                  createDataManifest = dataManifestWithNonExistingFile) {
                  invalidRequest2 =>
                    eventually {
                      assertSnsReceives(
                        Set(
                          ReplicationRequest(
                            archiveRequestId = validRequest1.id,
                            srcBagLocation = BagLocation(
                              storageNamespace = storageBucket.name,
                              storagePrefix = "archive",
                              storageSpace = validRequest1.storageSpace,
                              bagPath =
                                BagPath(bagInfo1.externalIdentifier.toString)
                            )
                          ),
                          ReplicationRequest(
                            archiveRequestId = validRequest2.id,
                            srcBagLocation = BagLocation(
                              storageNamespace = storageBucket.name,
                              storagePrefix = "archive",
                              storageSpace = validRequest2.storageSpace,
                              bagPath =
                                BagPath(bagInfo2.externalIdentifier.toString)
                            )
                          )
                        ),
                        nextTopic
                      )

                      assertTopicReceivesFailedProgress(
                        requestId = invalidRequest1.id,
                        expectedDescription =
                          "Failed reading file this/does/not/exists.jpg from zip file",
                        progressTopic = progressTopic
                      )

                      assertTopicReceivesFailedProgress(
                        requestId = invalidRequest2.id,
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
    val bagInfo1 = randomBagInfo
    val bagInfo2 = randomBagInfo

    withArchivist() {
      case (ingestBucket, storageBucket, queuePair, nextTopic, progressTopic) =>
        createAndSendBag(
          ingestBucket,
          queuePair,
          bagInfo = bagInfo1,
          dataFileCount = 1) { validRequest1 =>
          createAndSendBag(
            ingestBucket,
            queuePair,
            dataFileCount = 1,
            createBagInfoFile = _ => None) { invalidRequest1 =>
            createAndSendBag(
              ingestBucket,
              queuePair,
              bagInfo = bagInfo2,
              dataFileCount = 1) { validRequest2 =>
              createAndSendBag(
                ingestBucket,
                queuePair,
                dataFileCount = 1,
                createBagInfoFile = _ => None) { invalidRequest2 =>
                eventually {

                  assertQueuePairSizes(queuePair, 0, 0)

                  assertSnsReceives(
                    Set(
                      ReplicationRequest(
                        archiveRequestId = validRequest1.id,
                        srcBagLocation = BagLocation(
                          storageNamespace = storageBucket.name,
                          storagePrefix = "archive",
                          storageSpace = validRequest1.storageSpace,
                          bagPath =
                            BagPath(bagInfo1.externalIdentifier.toString)
                        )
                      ),
                      ReplicationRequest(
                        archiveRequestId = validRequest2.id,
                        srcBagLocation = BagLocation(
                          storageNamespace = storageBucket.name,
                          storagePrefix = "archive",
                          storageSpace = validRequest2.storageSpace,
                          bagPath =
                            BagPath(bagInfo2.externalIdentifier.toString)
                        )
                      )
                    ),
                    nextTopic
                  )

                  assertTopicReceivesFailedProgress(
                    requestId = invalidRequest1.id,
                    expectedDescription =
                      "Failed reading file bag-info.txt from zip file",
                    progressTopic = progressTopic
                  )

                  assertTopicReceivesFailedProgress(
                    requestId = invalidRequest2.id,
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
