package uk.ac.wellcome.platform.archive.common.progress

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{GetItemRequest, PutItemRequest, UpdateItemRequest}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{ProgressGenerators, ProgressTrackerFixture}
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.platform.archive.common.progress.monitor.{IdConstraintError, ProgressTracker}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb

import scala.util.Try

class ProgressTrackerTest
    extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with ProgressTrackerFixture
    with ProgressGenerators
    with ScalaFutures {

  import uk.ac.wellcome.storage.dynamo._
  import Progress._

  describe("create") {
    it("creates a progress monitor") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val progress = progressTracker.initialise(createProgress())

          assertTableOnlyHasItem(progress, table)
        }
      }
    }

    it("only allows the creation of one progress monitor for a given id") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val id = randomUUID

          val monitors = List(
            createProgressWith(id = id, uploadUri = testUploadUri),
            createProgressWith(id = id, uploadUri = testUploadUri)
          )

          val result = Try(monitors.map(progressTracker.initialise))
          val failedException = result.failed.get

          failedException shouldBe a[IdConstraintError]
          failedException.getMessage should include(
            s"There is already a progress tracker with id:$id")

          assertProgressCreated(id, testUploadUri, table)
        }

      }
    }

    it("throws if put to dynamo fails during creation") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        val mockDynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("root cause")
        when(mockDynamoDbClient.putItem(any[PutItemRequest]))
          .thenThrow(expectedException)

        val progressTracker = new ProgressTracker(
          mockDynamoDbClient,
          DynamoConfig(table = table.name, index = table.index)
        )

        val progress = createProgress()

        val result = Try(progressTracker.initialise(progress))
        val failedException = result.failed.get

        failedException shouldBe a[RuntimeException]
        failedException shouldBe expectedException
      }
    }
  }

  describe("read") {
    it("retrieves progress by id") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val progress = progressTracker.initialise(createProgress())
          assertTableOnlyHasItem[Progress](progress, table)

          val result = progressTracker.get(progress.id)
          result shouldBe a[Some[_]]
          result.get shouldBe progress
        }
      }
    }

    it("returns None when no progress monitor matches id") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val result = progressTracker.get(randomUUID)
          result shouldBe scala.None
        }
      }
    }

    it("throws when it encounters an error") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        val mockDynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("root cause")
        when(mockDynamoDbClient.getItem(any[GetItemRequest]))
          .thenThrow(expectedException)

        val progressTracker = new ProgressTracker(
          mockDynamoDbClient,
          DynamoConfig(table.name, table.index)
        )

        val result = Try(progressTracker.get(randomUUID))

        val failedException = result.failed.get

        failedException shouldBe expectedException
      }
    }
  }

  describe("update") {
    it("adds a single event to a monitor with no events") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val progress = progressTracker.initialise(createProgress())

          val progressUpdate = ProgressEventUpdate(
            progress.id,
            List(createProgressEvent)
          )

          progressTracker.update(progressUpdate)

          assertProgressCreated(progress.id, progress.uploadUri, table)

          assertProgressRecordedRecentEvents(
            progressUpdate.id,
            progressUpdate.events.map(_.description),
            table)
        }
      }
    }

    it("adds a status update to a monitor with no events") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val progress = progressTracker.initialise(createProgress())

          val progressUpdate = ProgressStatusUpdate(
            progress.id,
            Progress.Completed,
            List(createProgressEvent)
          )

          progressTracker.update(progressUpdate)

          val actualProgress =
            assertProgressCreated(progress.id, progress.uploadUri, table)

          actualProgress.status shouldBe Progress.Completed

          assertProgressRecordedRecentEvents(
            progressUpdate.id,
            progressUpdate.events.map(_.description),
            table)
        }
      }
    }

    it("adds a resource update to a monitor with no events") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val progress = progressTracker.initialise(
            createProgressWith(resources = List.empty))

          val resources = List(createResource)
          val progressUpdate = ProgressResourceUpdate(
            progress.id,
            resources,
            List(createProgressEvent)
          )

          progressTracker.update(progressUpdate)

          val actualProgress =
            assertProgressCreated(progress.id, progress.uploadUri, table)

          actualProgress.resources should contain theSameElementsAs resources

          assertProgressRecordedRecentEvents(
            progressUpdate.id,
            progressUpdate.events.map(_.description),
            table)
        }
      }
    }

    it("adds a callback status update to a monitor with no events") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val progress = progressTracker.initialise(createProgress())

          val progressUpdate = ProgressCallbackStatusUpdate(
            progress.id,
            Callback.Succeeded,
            List(createProgressEvent)
          )

          progressTracker.update(progressUpdate)

          val actualProgress =
            assertProgressCreated(progress.id, progress.uploadUri, table)

          actualProgress.callback shouldBe defined
          actualProgress.callback.get.status shouldBe Callback.Succeeded

          assertProgressRecordedRecentEvents(
            progressUpdate.id,
            progressUpdate.events.map(_.description),
            table)
        }
      }
    }

    it("adds an update with multiple events") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val progress = progressTracker.initialise(createProgress())

          val progressUpdate = ProgressEventUpdate(
            progress.id,
            List(createProgressEvent, createProgressEvent)
          )

          progressTracker.update(progressUpdate)

          assertProgressCreated(progress.id, progress.uploadUri, table)

          assertProgressRecordedRecentEvents(
            progressUpdate.id,
            progressUpdate.events.map(_.description),
            table)
        }
      }
    }

    it("adds multiple events to a monitor") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table) { progressTracker =>
          val progress = progressTracker.initialise(createProgress())

          val updates = List(
            createProgressEventUpdateWith(progress.id),
            createProgressEventUpdateWith(progress.id)
          )

          updates.foreach(progressTracker.update(_))

          assertProgressCreated(progress.id, progress.uploadUri, table)

          assertProgressRecordedRecentEvents(
            progress.id,
            updates.flatMap(_.events.map(_.description)),
            table)
        }
      }
    }

    it("throws if put to dynamo fails when adding an event") {
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        val mockDynamoDbClient = mock[AmazonDynamoDB]

        val expectedException = new RuntimeException("root cause")
        when(mockDynamoDbClient.updateItem(any[UpdateItemRequest]))
          .thenThrow(expectedException)
        val progressTracker = new ProgressTracker(
          mockDynamoDbClient,
          DynamoConfig(table = table.name, index = table.index)
        )

        val update = createProgressEventUpdateWith(randomUUID)

        val result = Try(progressTracker.update(update))

        val failedException = result.failed.get
        failedException shouldBe a[RuntimeException]
        failedException shouldBe expectedException
      }
    }
  }
}
