package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.net.URI
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.gu.scanamo.DynamoFormat
import org.scalatest.Assertion
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.flows.ProgressUpdateFlow
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
    with MockitoSugar
    with RandomThings
    with TimeTestFixture {

  import Progress._

  val uploadUri = new URI("http://www.example.com/asset")
  val callbackUri = new URI("http://localhost/archive/complete")

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
    callbackUrl: URI = callbackUri,
    uploadUrl: URI = uploadUri
  ): Progress = {
    val id = randomUUID

    progressMonitor.create(
      Progress(
        id = id,
        uploadUri = uploadUrl,
        callbackUri = Some(callbackUrl)
      ))
  }

  def givenProgressRecord(id: UUID,
                          uploadUri: URI,
                          maybeCallbackUri: Option[URI],
                          table: Table) = {
    givenTableHasItem(Progress(id, uploadUri, maybeCallbackUri), table)
  }

  def assertProgressCreated(id: UUID,
                            expectedUploadUri: URI,
                            expectedCallbackUri: Option[URI],
                            table: Table,
                            recentSeconds: Int = 45): Assertion = {
    val progress = getExistingTableItem[Progress](id.toString, table)
    progress.uploadUri shouldBe expectedUploadUri
    progress.callbackUri shouldBe expectedCallbackUri

    assertRecent(progress.createdDate, recentSeconds)
    assertRecent(progress.lastModifiedDate, recentSeconds)
  }

  def assertProgressRecordedRecentEvents(id: UUID,
                                         expectedEventDescriptions: Seq[String],
                                         table: LocalDynamoDb.Table,
                                         recentSeconds: Int = 45) = {
    val progress = getExistingTableItem[Progress](id.toString, table)

    progress.events.map(_.description) should contain theSameElementsAs expectedEventDescriptions
    progress.events.foreach(event =>
      assertRecent(event.createdDate, recentSeconds))
    progress
  }

  def assertProgressStatus(id: UUID,
                           expectedStatus: Status,
                           table: LocalDynamoDb.Table) = {
    val progress = getExistingTableItem[Progress](id.toString, table)

    progress.status shouldBe expectedStatus
  }
}
