package uk.ac.wellcome.platform.archive.common.progress

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.sns.model.PublishResult
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.flows.ProgressUpdateAndPublishFlow
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressEvent,
  ProgressUpdate
}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.test.fixtures.Akka

class ProgressUpdateAndPublishFlowTest
    extends FunSpec
    with LocalDynamoDb
    with MockitoSugar
    with Akka
    with SNS
    with ProgressMonitorFixture
    with ScalaFutures {

  import Progress._

  it("updates progress and publishes status") {
    withLocalSnsTopic { topic =>
      withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
        withProgressMonitor(table)(monitor => {
          withActorSystem(actorSystem => {
            withMaterializer(actorSystem)(materializer => {

              val flow = ProgressUpdateAndPublishFlow(
                snsClient,
                SNSConfig(topic.arn),
                monitor
              )

              val events = List(ProgressEvent("Run!"))
              val status = Progress.Failed

              val progress = createProgress(monitor, callbackUri, uploadUri)
              val update = ProgressUpdate(progress.id, events, status)

              val expectedProgress = progress.copy(
                events = progress.events ++ events,
                status = status
              )

              val source = Source.single(update)

              val eventualResult = source
                .via(flow)
                .async
                .runWith(Sink.head)(materializer)

              whenReady(eventualResult) {
                result =>
                  result shouldBe a[PublishResult]

                  assertSnsReceivesOnly(expectedProgress, topic)

                  assertProgressCreated(
                    progress.id,
                    uploadUri,
                    Some(callbackUri),
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
