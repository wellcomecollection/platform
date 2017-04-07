package uk.ac.wellcome.platform.idminter

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.util.Try

trait SNSLocal extends Suite with BeforeAndAfterEach {
  val localSNSEnpointUrl = "http://localhost:9292"
  val amazonSNS = AmazonSNSClientBuilder.standard().withEndpointConfiguration(new EndpointConfiguration(localSNSEnpointUrl, "local")).build()
  val ingestTopicArn = amazonSNS.createTopic("es_ingest").getTopicArn

  override def beforeEach(): Unit = {
    super.beforeEach()
    new DefaultHttpClient().execute(new HttpPost(localSNSEnpointUrl+"/drain"))
  }

}
