package uk.ac.wellcome.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.Encoder
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.util.hashing.MurmurHash3

class S3ObjectStore[T] @Inject()(
  s3Client: AmazonS3,
  s3Config: S3Config,
  keyPrefixGenerator: KeyPrefixGenerator[T]
) extends S3ObjectReader(s3Client, s3Config) {
  def put(sourcedObject: T)(
    implicit encoder: Encoder[T]): Future[S3ObjectLocation] = {
    val keyPrefix = keyPrefixGenerator.generate(sourcedObject)

    S3ObjectStore.put[T](s3Client, s3Config.bucketName)(keyPrefix)(
      sourcedObject)
  }
}

object S3ObjectStore extends Logging {
  def put[T](s3Client: AmazonS3, bucketName: String)(keyPrefix: String)(
    sourcedObject: T)(implicit encoder: Encoder[T]): Future[S3ObjectLocation] =
    Future.fromTry(JsonUtil.toJson(sourcedObject)).map { content =>
      val contentHash = MurmurHash3.stringHash(content, MurmurHash3.stringSeed)

      // Ensure that keyPrefix here is normalised for concatenating with contentHash
      val prefix = keyPrefix
        .stripPrefix("/")
        .stripSuffix("/")

      val key = s"$prefix/$contentHash.json"

      info(s"Attempting to PUT object to s3://$bucketName/$key")
      s3Client.putObject(bucketName, key, content)
      info(s"Successfully PUT object to s3://$bucketName/$key")

      S3ObjectLocation(bucketName, key)
    }
}
