package uk.ac.wellcome.test.utils

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sqs.AmazonSQS
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{IntegrationTest, TwitterModule}
import uk.ac.wellcome.finatra.modules._

trait IntegrationTestBase
    extends IntegrationTest
    with SQSLocal
    with DynamoDBLocal
    with SNSLocal {
  override def injector =
    TestInjector(
      flags = Map(
        "aws.region" -> "local",
        "aws.sqs.queue.url" -> idMinterQueueUrl,
        "sqs.waitTime" -> "1",
        "aws.sns.topic.arn" -> ingestTopicArn
      ),
      modules = Seq(AkkaModule,
                    LocalSNSClient,
                    DynamoDBLocalClientModule,
                    SQSReaderModule,
                    SQSLocalClientModule,
                    SNSConfigModule,
                    SQSConfigModule,
                    DynamoConfigModule)
    )

  object LocalSNSClient extends TwitterModule {

    @Singleton
    @Provides
    def providesSNSClient: AmazonSNS = amazonSNS
  }

  object DynamoDBLocalClientModule extends TwitterModule {

    @Singleton
    @Provides
    def providesDynamoDbClient: AmazonDynamoDB = dynamoDbClient
  }

  object SQSLocalClientModule extends TwitterModule {

    @Singleton
    @Provides
    def providesAmazonSQSClient: AmazonSQS = sqsClient
  }
}
