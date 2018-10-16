package uk.ac.wellcome.platform.archive.common.progress

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.sns.model.PublishResult
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{ProgressGenerators, ProgressTrackerFixture}
import uk.ac.wellcome.platform.archive.common.progress.flows.ProgressUpdateAndPublishFlow
import uk.ac.wellcome.platform.archive.common.progress.models.progress.{Progress, ProgressEvent, ProgressStatusUpdate}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.test.fixtures.Akka

class ProgressUpdateAndPublishFlowTest
    extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with Akka
    with SNS
    with ProgressTrackerFixture
    with ProgressGenerators
    with ScalaFutures {

  import Progress._

  it("updates progress and publishes status") {
    withLocalSnsTopic { topic =>
      withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
        withProgressTracker(table)( tracker => {
          withActorSystem(actorSystem => {
            withMaterializer(actorSystem)(materializer => {

              val flow = ProgressUpdateAndPublishFlow(
                snsClient,
                SNSConfig(topic.arn),
                tracker
              )

              val events = List(ProgressEvent("Run!"))
              val status = Progress.Failed

              val progress = createProgress()
              tracker.initialise(progress)
              val update = ProgressStatusUpdate(progress.id, status, events)

              val expectedProgress = progress.copy(
                events = progress.events ++ events,
                status = status
              )

              val eventualResult = Source
                .single(update)
                .via(flow)
                .async
                .runWith(Sink.head)(materializer)

              whenReady(eventualResult) {
                result =>
                  result shouldBe a[PublishResult]

                  assertSnsReceivesOnly(expectedProgress, topic)

                  assertProgressCreated(
                    progress.id,
                    progress.uploadUri,
                    table)

                  assertProgressRecordedRecentEvents(
                    progress.id,
                    update.events.map(_.description),
                    table
                  )

                  assertProgressStatus(
                    progress.id,
                    status,
                    table
                  )

              }
            })
          })
        })
      }
    }
  }
}
