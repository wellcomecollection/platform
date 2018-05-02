package uk.ac.wellcome.storage.vhs

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

object VHSConfigModule extends TwitterModule {
  private val tableName = flag[String](
    "aws.vhs.dynamo.tableName", "",
    "Name of the DynamoDB table holding the VHS index")

  private val bucketName = flag[String](
    "aws.vhs.s3.bucketName", "",
    "Name of the S3 bucket holding VHS objects")

  @Singleton
  @Provides
  def providesVHSConfig(): VHSConfig = {
    val dynamoConfig = DynamoConfig(table = tableName())
    val s3Config = S3Config(bucketName = bucketName())
    VHSConfig(dynamoConfig = dynamoConfig, s3Config = s3Config)
  }
}
