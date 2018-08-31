package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant}
import java.util.UUID

import com.gu.scanamo.DynamoFormat
import org.scalatest.Assertion
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.progress.models.ArchiveProgress
import uk.ac.wellcome.platform.archive.common.progress.models.ArchiveProgress.Status
import uk.ac.wellcome.platform.archive.common.progress.monitor.ArchiveProgressMonitor
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait ArchiveProgressMonitorFixture
  extends LocalProgressMonitorDynamoDb
    with MockitoSugar {

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, IllegalArgumentException]( str =>
      Instant.from(DateTimeFormatter.ISO_INSTANT.parse(str))
    )(
      DateTimeFormatter.ISO_INSTANT.format(_)
    )

  def withArchiveProgressMonitor[R](table: Table)(
    testWith: TestWith[ArchiveProgressMonitor, R]): R = {
    val archiveProgressMonitor = new ArchiveProgressMonitor(
      dynamoDbClient,
      DynamoConfig(table = table.name, index = table.index)
    )
    testWith(archiveProgressMonitor)
  }

  def withMockArchiveProgressMonitor[R]()(
    testWith: TestWith[ArchiveProgressMonitor, R]): R = {
    val archiveProgressMonitor = mock[ArchiveProgressMonitor]
    testWith(archiveProgressMonitor)
  }

  def givenArchiveProgressCreatedWith(uploadUrl: String, callbackUrl: String, archiveProgressMonitor: ArchiveProgressMonitor): ArchiveProgress = {
    val id = UUID.randomUUID().toString
    val eventualProgress = archiveProgressMonitor.initialize(ArchiveProgress(id, uploadUrl, Some(callbackUrl)))
    Await.result(eventualProgress, 500 millis)
  }

  def givenArchiveProgressRecord(id:String, uploadUrl: String, maybeCallbackUrl: Option[String], table: Table) = {
    givenTableHasItem(ArchiveProgress(id, uploadUrl, maybeCallbackUrl), table)
  }

  def assertProgressCreated(id: String, expectedUploadUrl: String, expectedCallbackUrl: Option[String], table: Table, recentSeconds: Int=45): Assertion = {
    val progress = getExistingTableItem[ArchiveProgress](id, table)
    progress.uploadUrl shouldBe expectedUploadUrl
    progress.callbackUrl shouldBe expectedCallbackUrl

    assertRecent(progress.createdAt, recentSeconds)
    assertRecent(progress.updatedAt, recentSeconds)
  }

  def assertProgressRecordedRecentEvents(id: String, expectedEventDescriptions: Seq[String], table: LocalDynamoDb.Table, recentSeconds: Int=45) = {
    val progress = getExistingTableItem[ArchiveProgress](id, table)

    progress.events.map(_.description) should contain theSameElementsAs expectedEventDescriptions
    progress.events.foreach(event => assertRecent(event.time, recentSeconds))
    progress
  }

  def assertProgressStatus(id: String, expectedStatus: Status, table: LocalDynamoDb.Table) = {
    val progress = getExistingTableItem[ArchiveProgress](id, table)

    progress.result shouldBe expectedStatus
  }

  def assertRecent(instant: Instant, recentSeconds: Int=1): Assertion =
      Duration.between(instant, Instant.now).getSeconds should be <= recentSeconds.toLong
}
