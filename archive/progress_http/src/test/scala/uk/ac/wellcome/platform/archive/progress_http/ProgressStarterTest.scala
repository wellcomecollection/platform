package uk.ac.wellcome.platform.archive.progress_http
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  StorageSpace
}
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  ProgressTrackerFixture
}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.dynamo._
import scala.concurrent.ExecutionContext.Implicits.global

class ProgressStarterTest
    extends FunSpec
    with ProgressTrackerFixture
    with SNS
    with ProgressGenerators
    with ScalaFutures
    with Matchers {

  it("saves a progress to the database and sends a ingest bag request to sns") {
    withLocalSnsTopic { topic =>
      withSNSWriter(topic) { writer =>
        withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
          withProgressTracker(table) { progressTracker =>
            val progressStarter =
              new ProgressStarter(progressTracker, writer)
            val progress = createProgress()

            whenReady(progressStarter.initialise(progress)) { p =>
              p shouldBe progress
              assertTableOnlyHasItem(progress, table)

              val requests =
                listMessagesReceivedFromSNS(topic).map(messageInfo =>
                  fromJson[IngestBagRequest](messageInfo.message).get)

              requests shouldBe List(IngestBagRequest(
                p.id,
                storageSpace = StorageSpace(p.space.underlying),
                archiveCompleteCallbackUrl = p.callback.map(_.uri),
                zippedBagLocation = ObjectLocation("ingest-bucket", "bag.zip")
              ))

            }

          }
        }
      }
    }
  }

}
