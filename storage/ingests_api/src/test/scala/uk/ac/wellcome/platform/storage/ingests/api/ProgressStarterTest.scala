package uk.ac.wellcome.platform.storage.ingests.api

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
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

class ProgressStarterTest
    extends FunSpec
    with ProgressTrackerFixture
    with SNS
    with ProgressGenerators
    with ScalaFutures
    with Matchers {

  val progress = createProgress

  it("saves a Progress to DynamoDB and send a notification to SNS") {
    withLocalSnsTopic { topic =>
      withProgressTrackerTable { table =>
        withProgressStarter(table, topic) { progressStarter =>
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

  it("returns a failed future if saving to DynamoDB fails") {
    withLocalSnsTopic { topic =>
      val fakeTable = Table("does-not-exist", index = "does-not-exist")
      withProgressStarter(fakeTable, topic) { progressStarter =>
        val future = progressStarter.initialise(progress)

        whenReady(future.failed) { _ =>
          assertSnsReceivesNothing(topic)
        }
      }
    }
  }

  it("returns a failed future if publishing to SNS fails") {
    withProgressTrackerTable { table =>
      val fakeTopic = Topic("does-not-exist")
      withProgressStarter(table, fakeTopic) { progressStarter =>
        val future = progressStarter.initialise(progress)

        whenReady(future.failed) { _ =>
          assertTableOnlyHasItem(progress, table)
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
