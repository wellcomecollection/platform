package uk.ac.wellcome.platform.idminter

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.AmazonSNSClientBuilder

trait SNSLocal  {
  val localSNSEnpointUrl = "http://localhost:5555"
  val amazonSNS = AmazonSNSClientBuilder.standard().withEndpointConfiguration(new EndpointConfiguration(localSNSEnpointUrl, "local")).build()
  val ingestTopicArn = amazonSNS.createTopic("es_ingest").getTopicArn
}
