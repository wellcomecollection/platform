package uk.ac.wellcome.platform.snapshot_convertor.services

import javax.inject.Inject

import akka.actor.ActorSystem
import com.amazonaws.services.s3.AmazonS3
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.snapshot_convertor.models.{CompletedConversionJob, ConversionJob}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.AwsRegionProvider
import com.twitter.inject.annotations.Flag
import io.circe.Json
import io.circe.parser.parse
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.models.{IdentifiedWork, WorksIncludes}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.io.Source

class ConvertorService @Inject()(
                                  @Flag("aws.s3.bucketName") bucketName: String,
                                  actorSystem: ActorSystem,
                                  awsConfig: AWSConfig,
                                  s3Client: AmazonS3) extends Logging {

  def runConversion(conversionJob: ConversionJob): Future[CompletedConversionJob] = {
    info(s"ConvertorService running $conversionJob")

    val credentialsProvider = DefaultAWSCredentialsProviderChain
      .getInstance()

    val regionProvider =
      new AwsRegionProvider {
        def getRegion: String = awsConfig.region
      }

    val materialiser = ActorMaterializer()

    val settings = new S3Settings(
      bufferType = MemoryBufferType,
      proxy = None,
      credentialsProvider = credentialsProvider,
      s3RegionProvider = regionProvider,
      pathStyleAccess = false,
      endpointUrl = None
    )

//    val s3Client = new S3Client(settings)(actorSystem, materialiser)

//
//    val (s3Source: Source[ByteString, _], _) = s3Client.download(bucket, bucketKey)
//
//    import akka.stream.scaladsl.Compression
//    val uncompressed = s3Source
//      .via(Compression.gunzip())
//      .map(_.utf8String)
//      .via(Framing.delimiter(ByteString("."), Int.MaxValue))
//      .map(JsonUtil.fromJson[IdentifiedWork])

//  CompletedConversionJob(conversionJob)


    val objectKey = conversionJob.objectKey

    // This should deal with compressed files
    val inputStream = s3Client.getObject(bucketName, objectKey)
      .getObjectContent()

    val sourceLines = Source.fromInputStream(inputStream).getLines()

    // we should do this in a streaming fashion
    val identifiedWorks = sourceLines.map(getResponseString => {
      val source: Json = (parse(getResponseString).right.get \\ "_source").head
      source.as[IdentifiedWork].right.get
    })

    // we need to make sure that all includes params are true
    val includes = WorksIncludes(
      identifiers = true,
      thumbnail = true,
      items = true
    )

    val displayWorks = identifiedWorks.map(identifiedWork => DisplayWork(
      work = identifiedWork,
      includes = includes
    ))

    // this should be Jackson
    val displayWorksString = displayWorks
      .map(displayWork => JsonUtil.toJson(displayWork).get)
      .mkString("\n")

    // this should put to a different bucket
    s3Client.putObject(bucketName, "target.txt", displayWorksString)

    // this should be an app variable
    val host = "http://localhost:33333"

    val targetLocation = Uri(s"$host/$objectKey")

    Future {
      CompletedConversionJob(
        conversionJob,
        targetLocation
      )
    }
  }
}
