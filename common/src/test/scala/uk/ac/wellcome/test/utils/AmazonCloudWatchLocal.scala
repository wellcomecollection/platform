package uk.ac.wellcome.test.utils

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder

trait AmazonCloudWatchLocal {
  val amazonCloudWatch = AmazonCloudWatchClientBuilder
    .standard()
    .withEndpointConfiguration(
      // use a fake endpoind in tests so that we don't send metrics to the real AWS
      new EndpointConfiguration("http://localhost:6789", "local"))
    .build()
}
