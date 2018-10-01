package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant}
import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.gu.scanamo.DynamoFormat
import org.scalatest.Assertion
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.progress.flows.ProgressUpdateFlow
import uk.ac.wellcome.platform.archive.common.progress.models.Progress.Status
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

trait ProgressMonitorFixture
    extends LocalProgressMonitorDynamoDb
    with MockitoSugar {

  val uploadUrl = "uploadUrl"
  val callbackUrl = "http://localhost/archive/complete"

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, IllegalArgumentException](str =>
      Instant.from(DateTimeFormatter.ISO_INSTANT.parse(str)))(
      DateTimeFormatter.ISO_INSTANT.format(_)
    )

  def withProgressMonitor[R](table: Table)(
    testWith: TestWith[ProgressMonitor, R]): R = {
    val progressMonitor = new ProgressMonitor(
      dynamoDbClient,
      DynamoConfig(table = table.name, index = table.index)
    )
    testWith(progressMonitor)
  }

  def withProgressUpdateFlow[R](table: Table)(
    testWith: TestWith[(
                         Flow[ProgressUpdate, Progress, NotUsed],
                         ProgressMonitor
                       ),
                       R]): R = {

    val progressMonitor = new ProgressMonitor(
      dynamoDbClient,
      DynamoConfig(table = table.name, index = table.index)
    )
    testWith((ProgressUpdateFlow(progressMonitor), progressMonitor))
  }

  def withMockProgressMonitor[R]()(
    testWith: TestWith[ProgressMonitor, R]): R = {
    val progressMonitor = mock[ProgressMonitor]
    testWith(progressMonitor)
  }

  def createProgress(
    progressMonitor: ProgressMonitor,
    callbackUrl: String = callbackUrl,
    uploadUrl: String = uploadUrl
  ): Progress = {
    val id = UUID.randomUUID().toString

    progressMonitor.create(
      Progress(id, uploadUrl, Some(callbackUrl))
    )
  }

  def givenProgressRecord(id: String,
                          uploadUrl: String,
                          maybeCallbackUrl: Option[String],
                          table: Table) = {
    givenTableHasItem(Progress(id, uploadUrl, maybeCallbackUrl), table)
  }

  def assertProgressCreated(id: String,
                            expectedUploadUrl: String,
                            expectedCallbackUrl: Option[String],
                            table: Table,
                            recentSeconds: Int = 45): Assertion = {
    val progress = getExistingTableItem[Progress](id, table)
    progress.uploadUrl shouldBe expectedUploadUrl
    progress.callbackUrl shouldBe expectedCallbackUrl

    assertRecent(progress.createdAt, recentSeconds)
    assertRecent(progress.updatedAt, recentSeconds)
  }

  def assertProgressRecordedRecentEvents(id: String,
                                         expectedEventDescriptions: Seq[String],
                                         table: LocalDynamoDb.Table,
                                         recentSeconds: Int = 45) = {
    val progress = getExistingTableItem[Progress](id, table)

    progress.events.map(_.description) should contain theSameElementsAs expectedEventDescriptions
    progress.events.foreach(event => assertRecent(event.time, recentSeconds))
    progress
  }

  def assertProgressStatus(id: String,
                           expectedStatus: Status,
                           table: LocalDynamoDb.Table) = {
    val progress = getExistingTableItem[Progress](id, table)

    progress.result shouldBe expectedStatus
  }

  def assertRecent(instant: Instant, recentSeconds: Int = 1): Assertion =
    Duration
      .between(instant, Instant.now)
      .getSeconds should be <= recentSeconds.toLong
}
