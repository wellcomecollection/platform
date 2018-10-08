package uk.ac.wellcome.platform.archive.notifier.fixtures

import java.net.URI

import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.platform.archive.notifier.modules.{
  ConfigModule,
  TestAppConfigModule
}
import uk.ac.wellcome.platform.archive.notifier.Notifier
import uk.ac.wellcome.platform.archive.common.fixtures.BagIt
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.TestWith

trait NotifierFixture extends S3 with Messaging with BagIt {

  protected val callbackHost = "localhost"
  protected val callbackPort = 8080

  val uploadUri = new URI(s"http://www.example.com/asset")

  def withApp[R](queue: Queue, topic: Topic)(
    testWith: TestWith[Notifier, R]): R = {
    val appConfigModule = new TestAppConfigModule(
      queue = queue,
      topic = topic
    )

    val injector: Injector = Guice.createInjector(
      appConfigModule,
      ConfigModule,
      AkkaModule,
      CloudWatchClientModule,
      SqsClientModule,
      SnsClientModule,
      MessageStreamModule
    )

    val app = injector.getInstance(classOf[Notifier])

    testWith(app)
  }

  def withNotifier[R](
    testWith: TestWith[(QueuePair, Topic, Notifier), R]): R = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic { topic =>
        withApp(queue = queuePair.queue, topic = topic) { app =>
          testWith((queuePair, topic, app))
        }
      }
    })
  }
}
