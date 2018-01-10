package uk.ac.wellcome.finatra.modules

import com.twitter.inject.TwitterModule

object S3ConfigModule extends TwitterModule {
  private val bucketName =
    flag[String]("aws.s3.bucketName", "", "Name of the S3 bucket")
}
