package uk.ac.wellcome.storage.s3

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.S3Config

object S3ConfigModule extends TwitterModule {

  private val bucketName =
    flag[String]("aws.s3.bucketName", "", "Name of the S3 bucket")
  private val endpoint = flag[String](
    "aws.s3.endpoint",
    "",
    "Endpoint of AWS S3. The region will be used if the endpoint is not provided")
  private val accessKey =
    flag[String]("aws.s3.accessKey", "", "AccessKey to access S3")
  private val secretKey =
    flag[String]("aws.s3.secretKey", "", "SecretKey to access S3")

  @Singleton
  @Provides
  def providesS3Config(): S3Config = S3Config(
    bucketName = bucketName(),
    endpoint = endpoint(),
    accessKey = accessKey(),
    secretKey = secretKey()
  )
}
