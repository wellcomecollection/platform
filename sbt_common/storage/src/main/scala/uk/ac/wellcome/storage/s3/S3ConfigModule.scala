package uk.ac.wellcome.storage.s3

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule

object S3ConfigModule extends TwitterModule {

  private val bucketName =
    flag[String]("aws.s3.bucketName", "", "Name of the S3 bucket")

  @Singleton
  @Provides
  def providesS3Config(): S3Config = S3Config(bucketName = bucketName())
}
