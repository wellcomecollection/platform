package uk.ac.wellcome.platform.archive.progress_async

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress => ProgressModel
}
import uk.ac.wellcome.platform.archive.progress_async.fixtures.{
  ProgressAsyncFixture => ProgressFixture
}

class ProgressAsyncFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressMonitorFixture
    with ProgressFixture
    with IntegrationPatience {

  import CallbackNotification._

  it("updates an existing progress monitor to Completed") {
    withConfiguredApp {
      case (qPair, topic, table, app) => {

        app.run()

        withProgressMonitor(table) { monitor =>
          withProgress(monitor) { progress =>
            withProgressUpdate(progress.id, ProgressModel.Completed) { update =>
              sendNotificationToSQS(qPair.queue, update)

              val updatedProgress = progress.update(update)
              val expectedNotification =
                CallbackNotification(
                  updatedProgress.id,
                  updatedProgress.callbackUri.get,
                  updatedProgress)

              eventually {
                assertSnsReceivesOnly(expectedNotification, topic)

                assertProgressCreated(
                  progress.id,
                  uploadUri,
                  Some(callbackUri),
                  table)

                assertProgressRecordedRecentEvents(
                  update.id,
                  update.events.map(_.description),
                  table
                )

              }
            }
          }
        }
      }
    }
  }
}
