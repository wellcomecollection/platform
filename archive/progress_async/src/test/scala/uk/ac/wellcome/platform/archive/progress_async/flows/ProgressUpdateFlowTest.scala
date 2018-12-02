package uk.ac.wellcome.platform.archive.progress_async.flows

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  ProgressTrackerFixture
}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  ProgressEvent,
  ProgressEventUpdate
}
import uk.ac.wellcome.platform.archive.progress_async.fixtures.ProgressAsyncFixture
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.test.fixtures.Akka

class ProgressUpdateFlowTest
    extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with Akka
    with RandomThings
    with ProgressTrackerFixture
    with ProgressGenerators
    with ProgressAsyncFixture
    with ScalaFutures {

  it("adds an event to a monitor with none") {
    withProgressTrackerTable { table =>
      withProgressUpdateFlow(table) {
        case (flow, monitor) =>
          withMaterializer { implicit materializer =>
            val progress = createProgress
            whenReady(monitor.initialise(progress)) { _ =>
              val update =
                ProgressEventUpdate(progress.id, List(ProgressEvent("Wow.")))

              val updates = Source
                .single(update)
                .via(flow)
                .async
                .runWith(Sink.ignore)

              whenReady(updates) { _ =>
                assertProgressCreated(progress, table)

                assertProgressRecordedRecentEvents(
                  update.id,
                  update.events.map(_.description),
                  table)
              }
            }
          }
      }
    }
  }

  it("adds multiple events to a monitor") {
    withProgressTrackerTable { table =>
      withProgressUpdateFlow(table) {
        case (flow, monitor) =>
          withMaterializer { implicit materializer =>
            val progress = createProgress
            monitor.initialise(progress)

            val progressUpdates = List(
              ProgressEventUpdate(
                progress.id,
                List(ProgressEvent("It happened again."))),
              ProgressEventUpdate(
                progress.id,
                List(ProgressEvent("Dammit Bobby.")))
            )

            val futureUpdates = Source
              .fromIterator(() => progressUpdates.toIterator)
              .via(flow)
              .async
              .runWith(Sink.ignore)

            whenReady(futureUpdates) { _ =>
              assertProgressCreated(progress, table)

              assertProgressRecordedRecentEvents(
                progress.id,
                progressUpdates.flatMap(_.events.map(_.description)),
                table)
            }

          }
      }
    }
  }

  it("continues on failure") {
    withProgressTrackerTable { table =>
      withProgressUpdateFlow(table) {
        case (flow, _) =>
          withMaterializer { implicit materializer =>
            val id = randomUUID

            val update =
              ProgressEventUpdate(
                id,
                List(ProgressEvent("Such progress, much wow.")))

            val updates = Source
              .single(update)
              .via(flow)
              .async
              .runWith(Sink.seq)

            whenReady(updates) { result =>
              result shouldBe empty
            }
          }
      }
    }
  }
}
