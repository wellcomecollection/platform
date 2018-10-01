package uk.ac.wellcome.platform.archive.call_backerei.fixtures

import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.platform.archive.call_backerei.modules.{ConfigModule, TestAppConfigModule}
import uk.ac.wellcome.platform.archive.call_backerei.CallBackerei
import uk.ac.wellcome.platform.archive.common.fixtures.BagIt
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.TestWith

trait CallBackereiFixture
    extends S3
    with Messaging
    with BagIt {

  def withApp[R](queue: Queue, topic: Topic)(testWith: TestWith[CallBackerei, R]): R = {
    val appConfigModule = new TestAppConfigModule(
      queue = queue,
      topic = topic
    )

    val injector: Injector = Guice.createInjector(
      appConfigModule,
      ConfigModule,
      AkkaModule,
      CloudWatchClientModule,
      SQSClientModule,
      SNSAsyncClientModule,
      MessageStreamModule
    )

    val app = injector.getInstance(classOf[CallBackerei])

    testWith(app)
  }

  def withCallBackerei[R](
    testWith: TestWith[(QueuePair, Topic, CallBackerei), R]): R = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic { topic =>
        withApp(queue = queuePair.queue, topic = topic) { app =>
          testWith((queuePair, topic, app))
        }
      }
    })
  }
}
