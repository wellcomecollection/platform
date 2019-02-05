package uk.ac.wellcome.platform.archive.common.progress

import java.time.Instant

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{GetItemRequest, PutItemRequest, UpdateItemRequest}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{ProgressGenerators, ProgressTrackerFixture}
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.platform.archive.common.progress.monitor.IdConstraintError
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class ProgressTrackerTest
    extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with ProgressTrackerFixture
    with ProgressGenerators
    with ScalaFutures {

  import uk.ac.wellcome.storage.dynamo._

  describe("create") {
    it("creates a progress monitor") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          val futureProgress = progressTracker.initialise(createProgress)

          whenReady(futureProgress) { progress =>
            assertTableOnlyHasItem(progress, table)
          }
        }
      }
    }

    it("only allows the creation of one progress monitor for a given id") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          val progress = createProgress

          val monitors = List(progress, progress)

          val result = Future.sequence(monitors.map(progressTracker.initialise))
          whenReady(result.failed) { failedException =>

            failedException shouldBe a[IdConstraintError]
            failedException.getMessage should include(
              s"There is already a progress tracker with id:${progress.id}")

            assertProgressCreated(progress, table)
          }
        }

      }
    }

    it("throws if put to dynamo fails during creation") {
      withProgressTrackerTable { table =>
        val mockDynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("root cause")
        when(mockDynamoDbClient.putItem(any[PutItemRequest]))
          .thenThrow(expectedException)

        withProgressTracker(table, dynamoDbClient = mockDynamoDbClient) { progressTracker =>
          val progress = createProgress

          val result = progressTracker.initialise(progress)
          whenReady(result.failed) { failedException =>
            failedException shouldBe a[RuntimeException]
            failedException shouldBe expectedException
          }
        }
      }
    }
  }

  describe("read") {
    it("retrieves progress by id") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          whenReady(progressTracker.initialise(createProgress)) { progress =>
            assertTableOnlyHasItem[Progress](progress, table)

            whenReady(progressTracker.get(progress.id)) { result =>
              result shouldBe a[Some[_]]
              result.get shouldBe progress
            }
          }
        }
      }
    }

    it("returns None when no progress monitor matches id") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          whenReady(progressTracker.get(randomUUID)) { result =>
            result shouldBe None
          }
        }
      }
    }

    it("throws when it encounters an error") {
      withProgressTrackerTable { table =>
        val mockDynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("root cause")
        when(mockDynamoDbClient.getItem(any[GetItemRequest]))
          .thenThrow(expectedException)

        withProgressTracker(table, dynamoDbClient = mockDynamoDbClient) { progressTracker =>
          whenReady(progressTracker.get(randomUUID).failed) { failedException =>
            failedException shouldBe expectedException
          }
        }
      }
    }
  }

  describe("update") {
    it("sets the bag id to a progress monitor with none") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          whenReady(progressTracker.initialise(createProgress)) { progress =>
            val bagId = createBagId

            val progressUpdate = ProgressStatusUpdate(
              progress.id,
              Progress.Processing,
              Some(bagId)
            )

            progressTracker.update(progressUpdate)

            val storedProgress = getExistingTableItem[Progress](progress.id.toString, table)

            assertRecent(storedProgress.createdDate)
            assertRecent(storedProgress.lastModifiedDate)
            storedProgress.events.map(_.description) should contain theSameElementsAs progressUpdate.events.map(_.description)
            storedProgress.events.foreach(event => assertRecent(event.createdDate))

            storedProgress.bag shouldBe Some(bagId)
          }
        }
      }
    }

    it("adds a single event to a monitor with no events") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          whenReady(progressTracker.initialise(createProgress)) { progress =>

            val progressUpdate = ProgressEventUpdate(
              progress.id,
              List(createProgressEvent)
            )

            progressTracker.update(progressUpdate)

            assertProgressCreated(progress, table)

            assertProgressRecordedRecentEvents(
              progressUpdate.id,
              progressUpdate.events.map(_.description),
              table)
          }
        }
      }
    }

    it("adds a status update to a monitor with no events") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          whenReady(progressTracker.initialise(createProgress)) { progress =>
            val someBagId = Some(randomBagId)
            val progressUpdate = ProgressStatusUpdate(
              progress.id,
              Progress.Completed,
              affectedBag = someBagId,
              List(createProgressEvent)
            )

            progressTracker.update(progressUpdate)

            val actualProgress = assertProgressCreated(progress, table)

            actualProgress.status shouldBe Progress.Completed
            actualProgress.bag shouldBe someBagId

            assertProgressRecordedRecentEvents(
              progressUpdate.id,
              progressUpdate.events.map(_.description),
              table)
          }
        }
      }
    }

    it("adds a callback status update to a monitor with no events") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          whenReady(progressTracker.initialise(createProgress)) { progress =>
            val progressUpdate = ProgressCallbackStatusUpdate(
              progress.id,
              Callback.Succeeded,
              List(createProgressEvent)
            )

            progressTracker.update(progressUpdate)

            val actualProgress = assertProgressCreated(progress, table)

            actualProgress.callback shouldBe defined
            actualProgress.callback.get.status shouldBe Callback.Succeeded

            assertProgressRecordedRecentEvents(
              progressUpdate.id,
              progressUpdate.events.map(_.description),
              table)
          }
        }
      }
    }

    it("adds an update with multiple events") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          whenReady(progressTracker.initialise(createProgress)) { progress =>
            val progressUpdate = ProgressEventUpdate(
              progress.id,
              List(createProgressEvent, createProgressEvent)
            )

            progressTracker.update(progressUpdate)

            assertProgressCreated(progress, table)

            assertProgressRecordedRecentEvents(
              progressUpdate.id,
              progressUpdate.events.map(_.description),
              table)
          }
        }
      }
    }

    it("adds multiple events to a monitor") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          whenReady(progressTracker.initialise(createProgress)) { progress =>
            val updates = List(
              createProgressEventUpdateWith(progress.id),
              createProgressEventUpdateWith(progress.id)
            )

            updates.foreach(progressTracker.update(_))

            assertProgressCreated(progress, table)

            assertProgressRecordedRecentEvents(
              progress.id,
              updates.flatMap(_.events.map(_.description)),
              table)
          }
        }
      }
    }

    it("throws if put to dynamo fails when adding an event") {
      withProgressTrackerTable { table =>
        val mockDynamoDbClient = mock[AmazonDynamoDB]

        val expectedException = new RuntimeException("root cause")
        when(mockDynamoDbClient.updateItem(any[UpdateItemRequest]))
          .thenThrow(expectedException)

        withProgressTracker(table, dynamoDbClient = mockDynamoDbClient) { progressTracker =>
          val update = createProgressEventUpdate

          val result = Try(progressTracker.update(update))

          val failedException = result.failed.get
          failedException shouldBe a[RuntimeException]
          failedException shouldBe expectedException
        }
      }
    }
  }

  describe("find progress by BagId") {
    it("query for multiple progresses for a same bag are returned in order of createdDate") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          val beforeTime = Instant.parse("2018-12-01T11:50:00.00Z")
          val time       = Instant.parse("2018-12-01T12:00:00.00Z")
          val afterTime  = Instant.parse("2018-12-01T12:10:00.00Z")
          whenReady(progressTracker.initialise(createProgressWith(createdDate=beforeTime))) { progressA =>
            whenReady(progressTracker.initialise(createProgressWith(createdDate=time))) { progressB =>
              whenReady(progressTracker.initialise(createProgressWith(createdDate=afterTime))) { progressC =>
                val bagId = createBagId

                val progressAUpdate = createProgressBagUpdateWith(progressA.id, bagId)
                progressTracker.update(progressAUpdate)
                val progressBUpdate = createProgressBagUpdateWith(progressB.id, bagId)
                progressTracker.update(progressBUpdate)
                val progressCUpdate = createProgressBagUpdateWith(progressC.id, bagId)
                progressTracker.update(progressCUpdate)

                val bagProgresses = progressTracker.findByBagId(bagId)

                bagProgresses shouldBe List(
                  Right(BagIngest(bagId.toString, progressC.id, afterTime)),
                  Right(BagIngest(bagId.toString, progressB.id, time)),
                  Right(BagIngest(bagId.toString, progressA.id, beforeTime)))
              }
            }
          }
        }
      }
    }

    it("only returns the most recent 30 progress entries") {
      withProgressTrackerTable { table =>
        withProgressTracker(table) { progressTracker =>
          val start = Instant.parse("2018-12-01T12:00:00.00Z")
          val eventualProgresses: Seq[Future[Progress]] =
            for (i <- 0 to 33) yield
              progressTracker.initialise(createProgressWith(createdDate=start.plusSeconds(i)))

          val bagId = createBagId

          eventualProgresses.map( eventualProgress =>
            eventualProgress.map { progress =>
              val progressUpdate = createProgressBagUpdateWith(progress.id, bagId)
              progressTracker.update(progressUpdate)
            })

          eventually {
            val bagProgresses = progressTracker.findByBagId(bagId)

            bagProgresses should have size 30
          }
        }
      }
    }
  }
}
