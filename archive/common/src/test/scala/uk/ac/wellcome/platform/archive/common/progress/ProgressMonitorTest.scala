package uk.ac.wellcome.platform.archive.common.progress

import java.util.UUID

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  GetItemRequest,
  PutItemRequest,
  UpdateItemRequest
}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressEvent,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.{
  IdConstraintError,
  ProgressMonitor
}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb

import scala.util.Try

class ProgressMonitorTest
    extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with ProgressMonitorFixture
    with ScalaFutures {

  import Progress._

  describe("create") {
    it("creates a progress monitor") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        withProgressMonitor(table) { archiveProgressMonitor =>
          val id = UUID.randomUUID()
          val archiveIngestProgress =
            Progress(id, uploadUri, Some(callbackUri), Progress.Processing)

          archiveProgressMonitor.create(archiveIngestProgress)
          assertTableOnlyHasItem(archiveIngestProgress, table)

        }
      }
    }

    it("only allows the creation of one progress monitor for a given id") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        withProgressMonitor(table) { archiveProgressMonitor =>
          val id = UUID.randomUUID()

          val monitors = List(
            Progress(id, uploadUri, Some(callbackUri)),
            Progress(id, uploadUri, Some(callbackUri))
          )

          val result = Try(monitors.map(archiveProgressMonitor.create))
          val failedException = result.failed.get

          failedException shouldBe a[IdConstraintError]
          failedException.getMessage should include(
            s"There is already a monitor with id:$id")

          assertProgressCreated(id, uploadUri, Some(callbackUri), table)
        }

      }
    }

    it("throws if put to dynamo fails during creation") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        val mockDynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("root cause")
        when(mockDynamoDbClient.putItem(any[PutItemRequest]))
          .thenThrow(expectedException)

        val archiveProgressMonitor = new ProgressMonitor(
          mockDynamoDbClient,
          DynamoConfig(table = table.name, index = table.index)
        )

        val id = UUID.randomUUID()
        val progress = Progress(id, uploadUri, Some(callbackUri))

        val result = Try(archiveProgressMonitor.create(progress))
        val failedException = result.failed.get

        failedException shouldBe a[RuntimeException]
        failedException shouldBe expectedException
      }
    }
  }

  describe("read") {
    it("retrieves progress by id") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        withProgressMonitor(table) { progressMonitor =>
          val progress =
            createProgress(progressMonitor, callbackUri, uploadUri)

          val result = progressMonitor.get(progress.id)

          assertTableOnlyHasItem(progress, table)

          result shouldBe a[Some[_]]
          result.get shouldBe progress
        }
      }
    }

    it("returns None when no progress monitor matches id") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        withProgressMonitor(table) { progressMonitor =>
          val progress =
            createProgress(progressMonitor, callbackUri, uploadUri)

          val result = progressMonitor.get(UUID.randomUUID())

          assertTableOnlyHasItem(progress, table)
          result shouldBe scala.None
        }
      }
    }

    it("throws when it encounters an error") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        val mockDynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("root cause")
        when(mockDynamoDbClient.getItem(any[GetItemRequest]))
          .thenThrow(expectedException)

        val archiveProgressMonitor = new ProgressMonitor(
          mockDynamoDbClient,
          DynamoConfig(table = table.name, index = table.index)
        )

        val id = UUID.randomUUID()
        val result = Try(archiveProgressMonitor.get(id))
        val failedException = result.failed.get

        failedException shouldBe a[RuntimeException]
        failedException shouldBe expectedException
      }
    }
  }

  describe("update") {

    it("adds an event to a monitor with none") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        withProgressMonitor(table) { archiveProgressMonitor =>
          val progress =
            createProgress(archiveProgressMonitor, callbackUri, uploadUri)

          val progressUpdate = ProgressUpdate(
            progress.id,
            List(ProgressEvent("So that happened."))
          )

          archiveProgressMonitor.update(progressUpdate)

          assertProgressCreated(
            progress.id,
            uploadUri,
            Some(callbackUri),
            table)
          assertProgressRecordedRecentEvents(
            progressUpdate.id,
            progressUpdate.events.map(_.description),
            table)

        }
      }
    }

    it("adds an update with multiple events") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        withProgressMonitor(table) { archiveProgressMonitor =>
          val progress =
            createProgress(archiveProgressMonitor, callbackUri, uploadUri)

          val progressUpdate = ProgressUpdate(
            progress.id,
            List(
              ProgressEvent("So that happened."),
              ProgressEvent("And then this"))
          )

          archiveProgressMonitor.update(progressUpdate)

          assertProgressCreated(
            progress.id,
            uploadUri,
            Some(callbackUri),
            table)
          assertProgressRecordedRecentEvents(
            progressUpdate.id,
            progressUpdate.events.map(_.description),
            table)

        }
      }
    }

    it("adds multiple events to a monitor") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        withProgressMonitor(table) { monitor: ProgressMonitor =>
          val progress = createProgress(monitor, callbackUri, uploadUri)

          val updates = List(
            ProgressUpdate(
              progress.id,
              List(ProgressEvent("It happened again."))
            ),
            ProgressUpdate(
              progress.id,
              List(ProgressEvent("Dammit Bobby."))
            )
          )

          updates.map(monitor.update)

          assertProgressCreated(
            progress.id,
            uploadUri,
            Some(callbackUri),
            table)
          assertProgressRecordedRecentEvents(
            progress.id,
            updates.flatMap(_.events.map(_.description)),
            table)
        }
      }
    }

    it("throws if put to dynamo fails when adding an event") {
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        val mockDynamoDbClient = mock[AmazonDynamoDB]

        val expectedException = new RuntimeException("root cause")
        when(mockDynamoDbClient.updateItem(any[UpdateItemRequest]))
          .thenThrow(expectedException)
        val archiveProgressMonitor = new ProgressMonitor(
          mockDynamoDbClient,
          DynamoConfig(table = table.name, index = table.index)
        )

        val id = UUID.randomUUID()

        val update =
          ProgressUpdate(id, List(ProgressEvent("Too much winning.")))

        val result = Try(archiveProgressMonitor.update(update))

        val failedException = result.failed.get

        failedException shouldBe a[RuntimeException]
        failedException shouldBe expectedException

      }
    }
  }
}
