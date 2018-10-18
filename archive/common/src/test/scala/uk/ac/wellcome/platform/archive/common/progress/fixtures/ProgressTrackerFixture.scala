package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.net.URI
import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.gu.scanamo.error.DynamoReadError
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.flows.ProgressUpdateFlow
import uk.ac.wellcome.platform.archive.common.progress.models.progress.Namespace
import uk.ac.wellcome.platform.archive.common.progress.models.progress.Callback
import uk.ac.wellcome.platform.archive.common.progress.models.progress.{
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

trait ProgressTrackerFixture
    extends LocalProgressTrackerDynamoDb
    with MockitoSugar
    with RandomThings
    with ProgressGenerators
    with TimeTestFixture {
  import uk.ac.wellcome.storage.dynamo._
  import Progress._

  def withProgressTracker[R](table: Table)(
    testWith: TestWith[ProgressTracker, R]): R = {
    val progressTracker = new ProgressTracker(
      dynamoDbClient,
      DynamoConfig(table = table.name, index = table.index)
    )
    testWith(progressTracker)
  }

  def withProgressUpdateFlow[R](table: Table)(
    testWith: TestWith[(
                         Flow[ProgressUpdate, Progress, NotUsed],
                         ProgressTracker
                       ),
                       R]): R = {

    val progressTracker = new ProgressTracker(
      dynamoDbClient,
      DynamoConfig(table = table.name, index = table.index)
    )
    testWith((ProgressUpdateFlow(progressTracker), progressTracker))
  }

  def withMockProgressTracker[R]()(
    testWith: TestWith[ProgressTracker, R]): R = {
    val progressTracker = mock[ProgressTracker]
    testWith(progressTracker)
  }

  def givenProgressRecord(
    id: UUID,
    uploadUri: URI,
    space: Namespace,
    maybeCallbackUri: Option[URI],
    table: Table): Option[Either[DynamoReadError, Progress]] = {
    givenTableHasItem(
      Progress(id, uploadUri, space, Callback(maybeCallbackUri)),
      table)
  }

  def assertProgressCreated(id: UUID,
                            expectedUploadUri: URI,
                            table: Table,
                            recentSeconds: Int = 45): Progress = {
    val progress = getExistingTableItem[Progress](id.toString, table)
    progress.uploadUri shouldBe expectedUploadUri

    assertRecent(progress.createdDate, recentSeconds)
    assertRecent(progress.lastModifiedDate, recentSeconds)
    progress
  }

  def assertProgressRecordedRecentEvents(id: UUID,
                                         expectedEventDescriptions: Seq[String],
                                         table: LocalDynamoDb.Table,
                                         recentSeconds: Int = 45) = {
    val progress = getExistingTableItem[Progress](id.toString, table)

    progress.events.map(_.description) should contain theSameElementsAs expectedEventDescriptions
    progress.events.foreach(event =>
      assertRecent(event.createdDate, recentSeconds))
  }

  def assertProgressStatus(id: UUID,
                           expectedStatus: Status,
                           table: LocalDynamoDb.Table) = {
    val progress = getExistingTableItem[Progress](id.toString, table)

    progress.status shouldBe expectedStatus
  }
}
