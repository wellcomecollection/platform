package uk.ac.wellcome.platform.archive.common.progress

import java.util.UUID

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{
  ProgressEvent,
  ProgressUpdate
}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.test.fixtures.Akka

class ProgressUpdateFlowTest
    extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with Akka
    with ProgressMonitorFixture
    with ScalaFutures {

  it("adds an event to a monitor with none") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressUpdateFlow(table) {
        case (flow, monitor) =>
          withActorSystem(actorSystem => {
            withMaterializer(actorSystem)(materializer => {
              val progress =
                createProgress(monitor, callbackUri, uploadUri)

              val update = ProgressUpdate(progress.id, ProgressEvent("Wow."))

              val updates = Source
                .single(update)
                .via(flow)
                .async
                .runWith(Sink.ignore)(materializer)

              whenReady(updates) { _ =>
                assertProgressCreated(
                  progress.id,
                  uploadUri,
                  Some(callbackUri),
                  table)
                assertProgressRecordedRecentEvents(
                  update.id,
                  Seq(update.event.description),
                  table)
              }
            })
          })
      }
    }
  }

  it("adds multiple events to a monitor") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressUpdateFlow(table) {
        case (flow, monitor) =>
          withActorSystem(actorSystem => {
            withMaterializer(actorSystem)(materializer => {

              val progress = createProgress(monitor, callbackUri, uploadUri)

              val events = List(
                ProgressUpdate(
                  progress.id,
                  ProgressEvent("It happened again.")),
                ProgressUpdate(progress.id, ProgressEvent("Dammit Bobby."))
              )

              val updates = Source
                .fromIterator(() => events.toIterator)
                .via(flow)
                .async
                .runWith(Sink.ignore)(materializer)

              whenReady(updates) { _ =>
                assertProgressCreated(
                  progress.id,
                  uploadUri,
                  Some(callbackUri),
                  table)
                assertProgressRecordedRecentEvents(
                  progress.id,
                  events.map(_.event.description),
                  table)
              }
            })
          })
      }
    }
  }

  it("continues on failure") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressUpdateFlow(table) {
        case (flow, monitor) =>
          withActorSystem(actorSystem => {
            withMaterializer(actorSystem)(materializer => {
              val id = UUID.randomUUID().toString

              val update =
                ProgressUpdate(id, ProgressEvent("Such progress, wow."))

              val updates = Source
                .single(update)
                .via(flow)
                .async
                .runWith(Sink.seq)(materializer)

              whenReady(updates) { result =>
                result shouldBe empty
              }
            })
          })
      }
    }
  }
}
