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
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, Update}
import uk.ac.wellcome.platform.archive.common.progress.monitor.{ProgressMonitor, IdConstraintError}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

//object ArchiveProgressUpdateFlow = {
//  def apply[T]() = {
//
//  }
//}

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
        val archiveIngestProgress = Progress(
          id,
          uploadUrl,
          Some(callbackUrl),
          Progress.Processing)

        whenReady(archiveProgressMonitor.create(archiveIngestProgress)) {
          _ =>
            assertTableOnlyHasItem(archiveIngestProgress, table)
        }
      }
    }
  }

  it("adds an event to a monitor with none") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressMonitor(table) { archiveProgressMonitor =>
        val progress = givenProgressCreatedWith(
          uploadUrl,
          callbackUrl,
          archiveProgressMonitor)

        val progressUpdate = Update(progress.id, "So that happened.")

        whenReady(archiveProgressMonitor.update(progressUpdate)) {
          _ =>
            assertProgressCreated(
              progress.id,
              uploadUrl,
              Some(callbackUrl),
              table = table)
            assertProgressRecordedRecentEvents(
              progressUpdate.id,
              Seq(progressUpdate.description),
              table)
        }
      }
    }
  }

  it("adds multiple events to a monitor") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressMonitor(table) { monitor: ProgressMonitor =>

        val progress = givenProgressCreatedWith(
          uploadUrl,
          callbackUrl,
          monitor
        )

        val updates = List(
          Update(progress.id, "It happened again."),
          Update(progress.id, "Dammit Bobby.")
        )

        val eventualEvents = Future.sequence(updates.map(monitor.update))

        whenReady(eventualEvents) { _ =>
          assertProgressCreated(
            progress.id,
            uploadUrl,
            Some(callbackUrl),
            table = table)
          assertProgressRecordedRecentEvents(
            progress.id,
            updates.map(_.description),
            table)
        }
      }
    }
  }

  it("only allows the creation of one progress monitor for a given id") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString

        val eventuallyCreated = Future.sequence(
          Seq(
            archiveProgressMonitor.create(
              Progress(id, uploadUrl, Some(callbackUrl))),
            archiveProgressMonitor.create(
              Progress(id, uploadUrl, Some(callbackUrl)))
          ))

        whenReady(eventuallyCreated.failed) { failedException =>
          failedException shouldBe a[IdConstraintError]
          failedException.getMessage should include(
            s"There is already a monitor with id:$id")

          assertProgressCreated(id, uploadUrl, Some(callbackUrl), table = table)
        }
      }
    }
  }

  it("throws if an event is added to progress that does not exist") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString

        val update = Update(id, "Such progress, wow.")

        whenReady(
          archiveProgressMonitor.update(update).failed) {
          failedException =>
            failedException shouldBe a[IdConstraintError]
            failedException.getMessage should include(
              s"Progress does not exist for id:$id")
        }
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

      whenReady(archiveProgressMonitor.create(progress).failed) {
        failedException =>
          failedException shouldBe a[RuntimeException]
          failedException shouldBe expectedException
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

      val id = UUID.randomUUID().toString

      val update = Update(id, "Too much winning.")

      whenReady(archiveProgressMonitor.update(update).failed) {
        failedException =>
          failedException shouldBe a[RuntimeException]
          failedException shouldBe expectedException
      }
    }
  }
}
