package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.util.UUID

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait ProgressTrackerFixture
    extends LocalProgressTrackerDynamoDb
    with RandomThings
    with ProgressGenerators
    with TimeTestFixture {

  import uk.ac.wellcome.storage.dynamo._

  def createTable(table: LocalDynamoDb.Table): Table = Table("table", "index")

  def withProgressTracker[R](table: Table, dynamoDbClient: AmazonDynamoDB = dynamoDbClient)(
    testWith: TestWith[ProgressTracker, R]): R = {
    val progressTracker = new ProgressTracker(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = createDynamoConfigWith(table)
    )
    testWith(progressTracker)
  }

  def assertProgressCreated(progress: Progress, table: Table): Progress = {
    val storedProgress = getExistingTableItem[Progress](progress.id.toString, table)
    storedProgress.sourceLocation shouldBe progress.sourceLocation

    assertRecent(storedProgress.createdDate, recentSeconds = 45)
    assertRecent(storedProgress.lastModifiedDate, recentSeconds = 45)
    storedProgress
  }

  def assertProgressRecordedRecentEvents(id: UUID,
                                         expectedEventDescriptions: Seq[String],
                                         table: LocalDynamoDb.Table): Unit = {
    val progress = getExistingTableItem[Progress](id.toString, table)

    progress.events.map(_.description) should contain theSameElementsAs expectedEventDescriptions
    progress.events.foreach(event =>
      assertRecent(event.createdDate, recentSeconds = 45))
  }

}
