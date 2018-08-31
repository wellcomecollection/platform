package uk.ac.wellcome.platform.archive.common.progress

import java.util.UUID

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  PutItemRequest,
  UpdateItemRequest
}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ArchiveProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.ArchiveProgress
import uk.ac.wellcome.platform.archive.common.progress.monitor.{
  ArchiveProgressMonitor,
  IdConstraintError
}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArchiveProgressMonitorTest
    extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with ArchiveProgressMonitorFixture
    with ScalaFutures {

  private val uploadUrl = "uploadUrl"
  private val callbackUrl = "http://localhost/archive/complete"

  it("creates a progress monitor") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withArchiveProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString
        val archiveIngestProgress = ArchiveProgress(
          id,
          uploadUrl,
          Some(callbackUrl),
          ArchiveProgress.Processing)

        whenReady(archiveProgressMonitor.initialize(archiveIngestProgress)) {
          _ =>
            assertTableOnlyHasItem(archiveIngestProgress, table)
        }
      }
    }
  }

  it("adds an event to a monitor with none") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withArchiveProgressMonitor(table) { archiveProgressMonitor =>
        val progress = givenArchiveProgressCreatedWith(
          uploadUrl,
          callbackUrl,
          archiveProgressMonitor)

        whenReady(archiveProgressMonitor.addEvent(progress.id, "This happened")) {
          _ =>
            assertProgressCreated(
              progress.id,
              uploadUrl,
              Some(callbackUrl),
              table = table)
            assertProgressRecordedRecentEvents(
              progress.id,
              Seq("This happened"),
              table)
        }
      }
    }
  }

  it("adds multiple events to a monitor") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withArchiveProgressMonitor(table) { archiveProgressMonitor =>
        val progress = givenArchiveProgressCreatedWith(
          uploadUrl,
          callbackUrl,
          archiveProgressMonitor)

        val eventualEvents = Future.sequence(
          Seq(
            archiveProgressMonitor.addEvent(progress.id, "This happened"),
            archiveProgressMonitor.addEvent(progress.id, "And this too")))

        whenReady(eventualEvents) { _ =>
          assertProgressCreated(
            progress.id,
            uploadUrl,
            Some(callbackUrl),
            table = table)
          assertProgressRecordedRecentEvents(
            progress.id,
            Seq("This happened", "And this too"),
            table)
        }
      }
    }
  }

  it("only allows the creation of one progress monitor for a given id") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withArchiveProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString

        val eventuallyCreated = Future.sequence(
          Seq(
            archiveProgressMonitor.initialize(
              ArchiveProgress(id, uploadUrl, Some(callbackUrl))),
            archiveProgressMonitor.initialize(
              ArchiveProgress(id, uploadUrl, Some(callbackUrl)))
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
      withArchiveProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString
        whenReady(
          archiveProgressMonitor.addEvent(id, "no matching monitor").failed) {
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
      val archiveProgressMonitor = new ArchiveProgressMonitor(
        mockDynamoDbClient,
        DynamoConfig(table = table.name, index = table.index)
      )

      val id = UUID.randomUUID().toString
      val archiveIngestProgress =
        ArchiveProgress(id, uploadUrl, Some(callbackUrl))

      whenReady(archiveProgressMonitor.initialize(archiveIngestProgress).failed) {
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
      val archiveProgressMonitor = new ArchiveProgressMonitor(
        mockDynamoDbClient,
        DynamoConfig(table = table.name, index = table.index)
      )

      val id = UUID.randomUUID().toString
      whenReady(archiveProgressMonitor.addEvent(id, "this should fail").failed) {
        failedException =>
          failedException shouldBe a[RuntimeException]
          failedException shouldBe expectedException
      }
    }
  }
}
