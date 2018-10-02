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

  private val uploadUrl = "uploadUrl"
  private val callbackUrl = "http://localhost/archive/complete"

  it("adds an event to a monitor with none") {
    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withProgressUpdateFlow(table) {
        case (flow, monitor) =>
          withActorSystem(actorSystem => {
            withMaterializer(actorSystem)(materializer => {
              val progress =
                createProgress(uploadUrl, callbackUrl, monitor)

              val update = ProgressUpdate(progress.id, List(ProgressEvent("Wow.")))

              val updates = Source
                .single(update)
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
                  update.id,
                  update.events.map(_.description),
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

              val progress = createProgress(
                uploadUrl,
                callbackUrl,
                monitor
              )

              val progressUpdates = List(
                ProgressUpdate(
                  progress.id,
                  List(ProgressEvent("It happened again."))),
                ProgressUpdate(progress.id, List(ProgressEvent("Dammit Bobby.")))
              )

              val futureUpdates = Source
                .fromIterator(() => progressUpdates.toIterator)
                .via(flow)
                .async
                .runWith(Sink.ignore)(materializer)

              whenReady(futureUpdates) { _ =>
                assertProgressCreated(
                  progress.id,
                  uploadUrl,
                  Some(callbackUrl),
                  table = table)
                assertProgressRecordedRecentEvents(
                  progress.id,
                  progressUpdates.flatMap(_.events.map(_.description)),
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
              val id = UUID.randomUUID()

              val update =
                ProgressUpdate(id, List(ProgressEvent("Such progress, wow.")))

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
