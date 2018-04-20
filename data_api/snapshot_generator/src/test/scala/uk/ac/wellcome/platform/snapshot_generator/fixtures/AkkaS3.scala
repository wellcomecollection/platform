package uk.ac.wellcome.platform.snapshot_generator.fixtures

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.AwsRegionProvider
import uk.ac.wellcome.platform.snapshot_generator.modules.AkkaS3ClientModule
import uk.ac.wellcome.test.fixtures.{S3, TestWith}

trait AkkaS3 extends S3 {

  def withS3AkkaClient[R](
    actorSystem: ActorSystem,
    materializer: ActorMaterializer)(testWith: TestWith[S3Client, R]): R = {

    val s3Settings = AkkaS3ClientModule.akkaS3Settings(
      credentialsProvider = new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(accessKey, secretKey)
      ),
      regionProvider = new AwsRegionProvider {
        def getRegion: String = regionName
      },
      endpointUrl = Some(localS3EndpointUrl)
    )

    val s3AkkaClient =
      new S3Client(s3Settings = s3Settings)(actorSystem, materializer)

    testWith(s3AkkaClient)
  }
}
