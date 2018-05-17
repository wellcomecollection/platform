package uk.ac.wellcome.finatra.storage

import javax.inject.Singleton

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.storage.s3.KeyPrefixGenerator
import uk.ac.wellcome.storage.vhs.{VHSConfig, VersionedHybridStore}

object VHSClientModule extends TwitterModule {
  override val modules = Seq(
    DynamoClientModule,
    S3ClientModule,
    VHSConfigModule
  )

  @Singleton
  @Provides
  def providesVHS[T](vhsConfig: VHSConfig,
                     s3Client: AmazonS3,
                     keyPrefixGenerator: KeyPrefixGenerator[T],
                     dynamoDbClient: AmazonDynamoDB): VersionedHybridStore[T] = new VersionedHybridStore[T](
    vhsConfig = vhsConfig,
    s3Client = s3Client,
    keyPrefixGenerator = keyPrefixGenerator,
    dynamoDbClient = dynamoDbClient
  )
}
