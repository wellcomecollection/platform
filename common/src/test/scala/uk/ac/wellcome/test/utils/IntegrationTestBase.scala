package uk.ac.wellcome.test.utils

import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sqs.AmazonSQS
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{IntegrationTest, TwitterModule}

trait IntegrationTestBase
    extends IntegrationTest
    with SQSLocal
    with DynamoDBLocal
    with SNSLocal {

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

  object LocalKinesisModule extends TwitterModule{

    @Provides
    @Singleton
    def provideAmazonKinesis: AmazonKinesis ={
      new AmazonDynamoDBStreamsAdapterClient(streamsClient)
    }
  }

  object SQSLocalClientModule extends TwitterModule {

    @Singleton
    @Provides
    def providesAmazonSQSClient: AmazonSQS = sqsClient
  }
}
