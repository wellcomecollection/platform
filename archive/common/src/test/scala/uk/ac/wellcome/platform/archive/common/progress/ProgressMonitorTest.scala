package uk.ac.wellcome.platform.archive.common.progress

import java.util.UUID

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{PutItemRequest, UpdateItemRequest}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressEvent, ProgressUpdate}
import uk.ac.wellcome.platform.archive.common.progress.monitor.{IdConstraintError, ProgressMonitor}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb

import scala.util.Try

class ProgressMonitorTest
    extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with ProgressMonitorFixture
    with ScalaFutures {

  private val uploadUrl = "uploadUrl"
  private val callbackUrl = "http://localhost/archive/complete"

  it("creates a progress monitor") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString
        val archiveIngestProgress = Progress(id, uploadUrl, Some(callbackUrl), Progress.Processing)

        archiveProgressMonitor.create(archiveIngestProgress)
        assertTableOnlyHasItem(archiveIngestProgress, table)

      }
    }
  }

  it("adds an event to a monitor with none") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressMonitor(table) { archiveProgressMonitor =>
        val progress = createProgress(
          uploadUrl,
          callbackUrl,
          archiveProgressMonitor)

        val progressUpdate = ProgressUpdate(
          progress.id,
          ProgressEvent("So that happened.")
        )

        archiveProgressMonitor.update(progressUpdate)

        assertProgressCreated(
          progress.id,
          uploadUrl,
          Some(callbackUrl),
          table = table)
        assertProgressRecordedRecentEvents(
          progressUpdate.id,
          Seq(progressUpdate.event.description),
          table)

      }
    }
  }

  it("adds multiple events to a monitor") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressMonitor(table) { monitor: ProgressMonitor =>
        val progress = createProgress(
          uploadUrl,
          callbackUrl,
          monitor
        )

        val updates = List(
          ProgressUpdate(
            progress.id,
            ProgressEvent("It happened again.")
          ),
          ProgressUpdate(
            progress.id,
            ProgressEvent("Dammit Bobby.")
          )
        )

        updates.map(monitor.update)

        assertProgressCreated(
          progress.id,
          uploadUrl,
          Some(callbackUrl),
          table = table)
        assertProgressRecordedRecentEvents(
          progress.id,
          updates.map(_.event.description),
          table)
      }
    }
  }

  it("only allows the creation of one progress monitor for a given id") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString

        val monitors = List(
          Progress(id, uploadUrl, Some(callbackUrl)),
          Progress(id, uploadUrl, Some(callbackUrl))
        )

        val result = Try(monitors.map(archiveProgressMonitor.create))
        val failedException = result.failed.get

        failedException shouldBe a[IdConstraintError]
        failedException.getMessage should include(
          s"There is already a monitor with id:$id")

        assertProgressCreated(id, uploadUrl, Some(callbackUrl), table = table)
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

      val id = UUID.randomUUID().toString
      val progress = Progress(id, uploadUrl, Some(callbackUrl))

      val result = Try(archiveProgressMonitor.create(progress))
      val failedException = result.failed.get

      failedException shouldBe a[RuntimeException]
      failedException shouldBe expectedException
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

      val id = UUID.randomUUID().toString

      val update = ProgressUpdate(id, ProgressEvent("Too much winning."))

      val result = Try(archiveProgressMonitor.update(update))

      val failedException = result.failed.get

      failedException shouldBe a[RuntimeException]
      failedException shouldBe expectedException

    }
  }
}
