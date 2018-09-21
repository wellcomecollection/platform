package uk.ac.wellcome.platform.archive.common.progress

import java.util.UUID

import akka.stream.scaladsl.{Flow, Sink, Source}
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.test.fixtures.Akka

class ProgressUpdateFlowTest
  extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with Akka
    with ProgressMonitorFixture
    with ScalaFutures {

  private val uploadUrl = "uploadUrl"
  private val callbackUrl = "http://localhost/archive/complete"

  it("adds an event to a monitor with none") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressUpdateFlow(table) { case (flow, monitor) =>
        withActorSystem(actorSystem => {
          withMaterializer(actorSystem)(materializer => {
            val progress = givenProgressCreatedWith(
              uploadUrl,
              callbackUrl,
              monitor)

            val progressUpdate = ProgressUpdate(progress.id, "Wow.")

            val updates = Source.single(progressUpdate)
              .via(flow)
              .async
              .runWith(Sink.ignore)(materializer)

            whenReady(updates) { _ =>
              assertProgressCreated(
                progress.id,
                uploadUrl,
                Some(callbackUrl),
                table = table)
              assertProgressRecordedRecentEvents(
                progressUpdate.id,
                Seq(progressUpdate.description),
                table)
            }
          })
        })
      }
    }
  }

  it("adds multiple events to a monitor") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressUpdateFlow(table) { case (flow, monitor) =>
        withActorSystem(actorSystem => {
          withMaterializer(actorSystem)(materializer => {

            val progress = givenProgressCreatedWith(
              uploadUrl,
              callbackUrl,
              monitor
            )

            val events = List(
              ProgressUpdate(progress.id, "It happened again."),
              ProgressUpdate(progress.id, "Dammit Bobby.")
            )

            val updates = Source.fromIterator(() => events.toIterator)
              .via(flow)
              .async
              .runWith(Sink.ignore)(materializer)

            whenReady(updates) { _ =>

              assertProgressCreated(
                progress.id,
                uploadUrl,
                Some(callbackUrl),
                table = table)
              assertProgressRecordedRecentEvents(
                progress.id,
                events.map(_.description),
                table)
            }
          })
        })
      }
    }
  }

  it("materializes a Left[FailedProgressUpdate] if an update fails") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressUpdateFlow(table) { case (flow, monitor) =>
        withActorSystem(actorSystem => {
          withMaterializer(actorSystem)(materializer => {
            val id = UUID.randomUUID().toString

            val progressUpdate = ProgressUpdate(id, "Such progress, wow.")

            val updates = Source.single(progressUpdate)
              .via(flow)
              .async
              .runWith(Sink.head)(materializer)

            whenReady(updates) { result =>
              result.isLeft shouldBe true
              result.left.get.e.getMessage should include(
                s"Progress does not exist for id:$id"
              )
            }
          })
        })
      }
    }
  }
}
