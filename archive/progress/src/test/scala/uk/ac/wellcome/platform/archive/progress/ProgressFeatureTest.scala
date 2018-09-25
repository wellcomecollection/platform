package uk.ac.wellcome.platform.archive.progress

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress => ProgressModel
}
import uk.ac.wellcome.platform.archive.progress.fixtures.{
  Progress => ProgressFixture
}
import uk.ac.wellcome.test.utils.ExtendedPatience

class ProgressFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressMonitorFixture
    with ProgressFixture
    with ExtendedPatience {

  it("updates an existing progress monitor to Completed") {
    withProgressApp {
      case (qPair, topic, table, app) => {

        app.run()

        withProgressMonitor(table) { monitor =>
          withProgress(monitor) { progress =>
            withProgressUpdate(progress.id, ProgressModel.Completed) { update =>
              sendNotificationToSQS(qPair.queue, update)

              val expectedProgress = progress.update(update)

              eventually {

                assertSnsReceivesOnly(expectedProgress, topic)
                assertProgressCreated(
                  progress.id,
                  uploadUrl,
                  Some(callbackUrl),
                  table = table
                )

                assertProgressRecordedRecentEvents(
                  update.id,
                  Seq(update.event.description),
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
