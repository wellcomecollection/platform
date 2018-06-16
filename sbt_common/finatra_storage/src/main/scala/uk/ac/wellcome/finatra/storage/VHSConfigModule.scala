package uk.ac.wellcome.finatra.storage

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.vhs.VHSConfig

object VHSConfigModule extends TwitterModule {
  private val tableName = flag[String](
    "aws.vhs.dynamo.tableName",
    "Name of the DynamoDB table holding the VHS index")

  private val bucketName = flag[String](
    "aws.vhs.s3.bucketName",
    "Name of the S3 bucket holding VHS objects")

  private val globalS3Prefix = flag[String](
    name = "aws.vhs.s3.globalPrefix",
    help = "A string prepended to all objects in the VHS"
  )

  @Singleton
  @Provides
  def providesVHSConfig(): VHSConfig = {
    val dynamoConfig = DynamoConfig(table = tableName())
    val s3Config = S3Config(bucketName = bucketName())
    VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config,
      globalS3Prefix = globalS3Prefix()
    )
  }
}
