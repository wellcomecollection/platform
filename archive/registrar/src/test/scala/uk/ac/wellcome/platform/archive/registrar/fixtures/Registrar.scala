package uk.ac.wellcome.platform.archive.registrar.fixtures

import com.google.inject.Guice
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.messaging.fixtures.AkkaS3
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.modules.{ConfigModule, TestAppConfigModule}
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.platform.archive.registrar.{Registrar => RegistrarApp}

trait Registrar extends AkkaS3 with Messaging {

  def withApp[R](storageBucket: Bucket, queuePair: QueuePair, topicArn: Topic)(
    testWith: TestWith[RegistrarApp, R]) = {
    val registrar = new RegistrarApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          queuePair.queue.url,
          storageBucket.name,
          topicArn.arn),
        ConfigModule,
        AkkaModule,
        AkkaS3ClientModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSAsyncClientModule
      )
    }
    testWith(registrar)
  }

  def withRegistrar[R](
                        testWith: TestWith[(Bucket, Bucket, QueuePair, Topic, RegistrarApp), R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic { snsTopic =>
        withLocalS3Bucket { ingestBucket =>
          withLocalS3Bucket { storageBucket =>
            withApp(storageBucket, queuePair, snsTopic) { registrar =>
              testWith(
                (ingestBucket, storageBucket, queuePair, snsTopic, registrar))
            }
          }
        }
      }
    })
  }
}
