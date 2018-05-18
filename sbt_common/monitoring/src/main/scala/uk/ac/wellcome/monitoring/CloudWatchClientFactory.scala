package uk.ac.wellcome.monitoring

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.{
  AmazonCloudWatch,
  AmazonCloudWatchClientBuilder
}

object CloudWatchClientFactory {
  def create(region: String, endpoint: String): AmazonCloudWatch = {
    val standardClient = AmazonCloudWatchClientBuilder.standard
    if (endpoint.isEmpty)
      standardClient
        .withRegion(region)
        .build()
    else
      standardClient
        .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
        .build()
  }
}
