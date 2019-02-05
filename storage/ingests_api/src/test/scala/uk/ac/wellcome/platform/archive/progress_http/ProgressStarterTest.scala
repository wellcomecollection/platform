package uk.ac.wellcome.platform.archive.progress_http

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SNS
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  StorageSpace
}
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  ProgressTrackerFixture
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

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
      withProgressTrackerTable { table =>
        withProgressStarter(table, topic) { progressStarter =>
          val progress = createProgress

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
              zippedBagLocation = ObjectLocation(
                progress.sourceLocation.location.namespace,
                progress.sourceLocation.location.key)
            ))
          }
        }
      }
    }
  }

  private def withProgressStarter[R](table: Table, topic: Topic)(
    testWith: TestWith[ProgressStarter, R]): R =
    withSNSWriter(topic) { snsWriter =>
      withProgressTracker(table) { progressTracker =>
        val progressStarter = new ProgressStarter(
          progressTracker = progressTracker,
          snsWriter = snsWriter
        )

        testWith(progressStarter)
      }
    }
}
