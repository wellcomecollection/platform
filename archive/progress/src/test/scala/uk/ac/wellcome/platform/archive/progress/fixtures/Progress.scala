package uk.ac.wellcome.platform.archive.progress.fixtures

import java.util.UUID

import com.google.inject.Guice
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.fixtures.AkkaS3
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models
import uk.ac.wellcome.platform.archive.common.progress.models.{ProgressEvent, ProgressUpdate, Progress => ProgressModel}
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.platform.archive.progress.modules.{ConfigModule, TestAppConfigModule}
import uk.ac.wellcome.platform.archive.progress.{Progress => ProgressApp}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

import scala.util.Random

trait Progress
  extends AkkaS3
    with LocalDynamoDb
    with ProgressMonitorFixture
    with Messaging {

  val uploadUrl = "uploadUrl"
  val callbackUrl = "http://localhost/archive/complete"

  private def randomAlphanumeric(length: Int = 8) = {
    Random.alphanumeric take length mkString
  }

  def withProgress[R](monitor: ProgressMonitor)(testWith: TestWith[models.Progress, R]) = {
    val id = UUID.randomUUID().toString
    val createdProgress = ProgressModel(id, uploadUrl, Some(callbackUrl))

    val storedProgress = monitor.create(createdProgress)

    testWith(storedProgress)
  }

  def withProgressUpdate[R](id: String, status: ProgressModel.Status = ProgressModel.None)(testWith: TestWith[ProgressUpdate, R]) = {

    val event = ProgressEvent(
      description = randomAlphanumeric()
    )

    val progress = ProgressUpdate(
      id = id,
      event = event,
      status = status
    )

    testWith(progress)
  }

  def withApp[R](queuePair: QueuePair,
                 topicArn: Topic,
                 progressTable: Table)(testWith: TestWith[ProgressApp, R]) = {
    val progress = new ProgressApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          queuePair.queue.url,
          topicArn.arn,
          progressTable
        ),
        ConfigModule,
        AkkaModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSAsyncClientModule,
        ProgressMonitorModule
      )
    }
    testWith(progress)
  }

  def withProgressApp[R](
                          testWith: TestWith[(QueuePair, Topic, Table, ProgressApp),
                            R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic {
        snsTopic =>

          withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) {
            progressTable =>
              withApp(queuePair, snsTopic, progressTable) {
                archivist =>
                  testWith(
                    (

                      queuePair,
                      snsTopic,
                      progressTable,
                      archivist))
              }

          }
      }
    })
  }
}
