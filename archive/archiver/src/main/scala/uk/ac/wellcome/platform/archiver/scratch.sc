import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.S3Object
import uk.ac.wellcome.platform.archiver.AkkaS3ClientModule
import uk.ac.wellcome.storage.s3.S3ClientFactory

import scala.concurrent._
import scala.concurrent.duration._

import ExecutionContext.Implicits.global

implicit val system = ActorSystem("system")
implicit val materializer = ActorMaterializer()

val localS3EndpointUrl = "http://localhost:33333"
val regionName = "localhost"
val accessKey = "accessKey1"
val secretKey = "verySecretKey1"

val s3Client = S3ClientFactory.create(
  region = regionName,
  endpoint = localS3EndpointUrl,
  accessKey = accessKey,
  secretKey = secretKey
)

val akkaS3Client = AkkaS3ClientModule.buildAkkaS3Client(
  regionName,
  system,
  localS3EndpointUrl,
  accessKey,
  secretKey
)

val bucket = "bucket"
val bucketKey = "key"
val body = "body"

s3Client.createBucket(bucket)

val s3Sink = akkaS3Client.multipartUpload(bucket, bucketKey)
val (s3Source: Source[ByteString, _], _) = akkaS3Client.download(bucket, bucketKey)

val result = for {
  _ <- Source.single(ByteString(body)).runWith(s3Sink)
  d1 <- Future (s3Client.getObject(bucket, bucketKey))
    .map(o => scala.io.Source.fromInputStream(o.getObjectContent).mkString)
  d2 <- s3Source.map(_.utf8String).runWith(Sink.head)
} yield (d1,d2)

val awaited = Await.result(result, 5 seconds)
