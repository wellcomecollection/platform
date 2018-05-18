package uk.ac.wellcome.finatra.storage

import javax.inject.Singleton

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.storage.s3.S3ClientFactory

object S3ClientModule extends TwitterModule {

  private val endpoint = flag[String](
    "aws.s3.endpoint",
    "",
    "Endpoint of AWS S3. The region will be used if the endpoint is not provided")
  private val accessKey =
    flag[String]("aws.s3.accessKey", "", "AccessKey to access S3")
  private val secretKey =
    flag[String]("aws.s3.secretKey", "", "SecretKey to access S3")

  private val region = flag[String]("aws.s3.region", "eu-west-1")

  @Singleton
  @Provides
  def providesS3Client(): AmazonS3 =
    S3ClientFactory.create(
      region = region(),
      endpoint = endpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )
}
