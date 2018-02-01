package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig

object S3ClientModule extends TwitterModule {
  val s3Endpoint = flag[String](
    "aws.s3.endpoint",
    "",
    "Endpoint of AWS S3. The region will be used if the endpoint is not provided")
  private val accessKey =
    flag[String]("aws.s3.accessKey", "", "AccessKey to access S3")
  private val secretKey =
    flag[String]("aws.s3.secretKey", "", "SecretKey to access S3")

  @Singleton
  @Provides
  def providesS3Client(awsConfig: AWSConfig): AmazonS3 = {
    val standardClient = AmazonS3ClientBuilder.standard
    if (s3Endpoint().isEmpty)
      standardClient
        .withRegion(awsConfig.region)
        .build()
    else
      standardClient
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey(), secretKey())))
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(new EndpointConfiguration(s3Endpoint(),
                                                             awsConfig.region))
        .build()
  }

}
