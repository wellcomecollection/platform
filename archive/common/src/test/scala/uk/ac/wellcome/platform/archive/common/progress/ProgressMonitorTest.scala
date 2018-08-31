package uk.ac.wellcome.platform.archive.common.progress

import java.time.{Duration, Instant}
import java.util.UUID

import com.gu.scanamo.Scanamo
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ArchiveProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.ArchiveProgress
import uk.ac.wellcome.platform.archive.common.progress.monitor.IdConstraintError
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class ProgressMonitorTest
    extends FunSpec
    with LocalDynamoDb
    with ArchiveProgressMonitorFixture
    with ScalaFutures {

  it("creates a progress monitor") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withArchiveProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString
        val archiveProgress = ArchiveProgress(
          id,
          "uploadUrl",
          Some("http://localhost/archive/complete"))

        archiveProgressMonitor.initialize(archiveProgress)

        assertTableOnlyHasItem(archiveProgress, table)
      }
    }
  }

  it("only allows the creation of one progress monitor for a given id") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withArchiveProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString
        val archiveIngestProgress = ArchiveProgress(
          id,
          "uploadUrl",
          Some("http://localhost/archive/complete"))

        givenTableHasItem(archiveIngestProgress, table)

        whenReady(
          archiveProgressMonitor.initialize(archiveIngestProgress).failed) {
          failedException =>
            failedException shouldBe a[IdConstraintError]
            assertTableOnlyHasItem(archiveIngestProgress, table)
        }

      }
    }
  }

  it("adds an event to a monitor with none") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withArchiveProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString
        val archiveIngestProgress = ArchiveProgress(
          id,
          "uploadUrl",
          Some("http://localhost/archive/complete"))

        whenReady(
          Future.sequence(
            Seq(
              archiveProgressMonitor.initialize(archiveIngestProgress),
              archiveProgressMonitor.addEvent(id, "This happened")))) { _ =>
          val records =
            Scanamo.scan[ArchiveProgress](dynamoDbClient)(table.name)
          records.size shouldBe 1
          val progress = records.head.right.get

          progress.events.size shouldBe 1
          progress.events.head.description shouldBe "This happened"
          Duration
            .between(progress.events.head.time, Instant.now)
            .getSeconds should be <= 1L
        }
      }
    }
  }

  it("adds two events to a monitor") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withArchiveProgressMonitor(table) { archiveProgressMonitor =>
        val id = UUID.randomUUID().toString
        val archiveIngestProgress = ArchiveProgress(
          id,
          "uploadUrl",
          Some("http://localhost/archive/complete"))

        whenReady(
          Future.sequence(Seq(
            archiveProgressMonitor.initialize(archiveIngestProgress),
            archiveProgressMonitor.addEvent(id, "This happened"),
            archiveProgressMonitor.addEvent(id, "And this too")
          ))) { _ =>
          val records =
            Scanamo.scan[ArchiveProgress](dynamoDbClient)(table.name)
          records.size shouldBe 1
          val progress = records.head.right.get

          progress.events.size shouldBe 2
          progress.events.head.description shouldBe "This happened"
          Duration
            .between(progress.events.head.time, Instant.now)
            .getSeconds should be <= 1L
          progress.events(1).description shouldBe "And this too"
          Duration
            .between(progress.events(1).time, Instant.now)
            .getSeconds should be <= 1L
        }
      }
    }
  }
}
