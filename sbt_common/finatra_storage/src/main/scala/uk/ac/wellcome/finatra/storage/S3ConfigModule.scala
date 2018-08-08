package uk.ac.wellcome.finatra.storage

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.storage.s3.S3Config

object S3ConfigModule extends TwitterModule {
  private val bucketName =
    flag[String](name = "aws.s3.bucketName", help = "Name of the S3 bucket")

  @Singleton
  @Provides
  def providesS3Config(): S3Config = S3Config(bucketName = bucketName())
}
