package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.net.URI
import java.util.UUID

import com.gu.scanamo.error.DynamoReadError
import org.scalatest.Assertion
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.Namespace
import uk.ac.wellcome.platform.archive.common.progress.models.{Callback, Progress, StorageLocation}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

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

  def withMockProgressTracker[R]()(
    testWith: TestWith[ProgressTracker, R]): R = {
    val progressTracker = mock[ProgressTracker]
    testWith(progressTracker)
  }

  def givenProgressRecord(
    id: UUID,
    storageLocation: StorageLocation,
    space: Namespace,
    maybeCallbackUri: Option[URI],
    table: Table): Option[Either[DynamoReadError, Progress]] = {
    val progress = Progress(
      id = id,
      sourceLocation = storageLocation,
      space = space,
      callback = Callback(maybeCallbackUri)
    )
    givenTableHasItem(progress, table = table)
  }

  def assertProgressCreated(id: UUID,
                            expectedStorageLocation: StorageLocation,
                            table: Table,
                            recentSeconds: Int = 45): Progress = {
    val progress = getExistingTableItem[Progress](id.toString, table)
    progress.sourceLocation shouldBe expectedStorageLocation

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
                           table: LocalDynamoDb.Table): Assertion = {
    val progress = getExistingTableItem[Progress](id.toString, table)

    progress.status shouldBe expectedStatus
  }
}
