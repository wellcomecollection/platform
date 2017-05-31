package uk.ac.wellcome.transformer.modules

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.kinesis.AmazonKinesis
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule

object AmazonKinesisModule extends TwitterModule {

  @Provides
  @Singleton
  def providesAmazonKinesis(
    dynamoDbStreamsClient: AmazonDynamoDBStreams): AmazonKinesis =
    new AmazonDynamoDBStreamsAdapterClient(dynamoDbStreamsClient)

}
