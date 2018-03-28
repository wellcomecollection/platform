package uk.ac.wellcome.platform.snapshot_convertor.fixtures

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.AwsRegionProvider
import uk.ac.wellcome.test.fixtures.{S3, TestWith}

trait AkkaS3 extends S3 {

  def withS3AkkaClient[R](
    actorSystem: ActorSystem,
    materializer: Materializer)(testWith: TestWith[S3Client, R]): R = {
    val s3AkkaClient = new S3Client(
      new S3Settings(
        bufferType = MemoryBufferType,
        proxy = None,
        credentialsProvider = new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey, secretKey)
        ),
        s3RegionProvider = new AwsRegionProvider {
          def getRegion: String = regionName
        },
        pathStyleAccess = true,
        endpointUrl = Some(localS3EndpointUrl)
      )
    )(actorSystem, materializer)

    testWith(s3AkkaClient)
  }
}
