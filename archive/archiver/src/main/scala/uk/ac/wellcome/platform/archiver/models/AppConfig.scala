package uk.ac.wellcome.platform.archiver.models

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sqs.SQSConfig

import scala.concurrent.duration._

class AppConfig(arguments: Seq[String]) extends ScallopConf(arguments) {

  val awsS3AccessKey = opt[String]()
  val awsS3SecretKey = opt[String]()
  val awsS3Region = opt[String](default = Some("eu-west-1"))
  val awsS3Endpoint = opt[String]()

  val awsSQSAccessKey = opt[String]()
  val awsSQSSecretKey = opt[String]()
  val awsSQSRegion = opt[String](default = Some("eu-west-1"))
  val awsSQSEndpoint = opt[String]()

  val awsCloudWatchRegion = opt[String](default = Some("eu-west-1"))
  val awsCloudWatchEndpoint = opt[String]()

  val sqsQueueUrl: ScallopOption[String] = opt[String](required = true)
  val sqsWaitTimeSeconds = opt[Int](required = true, default = Some(20))
  val sqsMaxMessages = opt[Int](required = true, default = Some(10))
  val sqsParallelism = opt[Int](required = true, default = Some(10))


  //  private val uploadNamespace = opt[String]()
  //
  //  private val uploadPrefix = opt[String](default = Some("archive"))
  //  private val digestDelimiter = opt[String](default = Some("  "))

  verify()

  val digestNames: List[String] = List(
    "manifest-md5.txt",
    "tagmanifest-md5.txt"
  )

  val s3ClientConfig = S3ClientConfig(
    accessKey = awsS3AccessKey.toOption,
    secretKey = awsS3SecretKey.toOption,
    region = awsS3Region(),
    endpoint = awsS3Endpoint.toOption
  )

  val cloudwatchClientConfig = CloudwatchClientConfig(
    region = awsCloudWatchRegion(),
    endpoint = awsCloudWatchEndpoint.toOption
  )

  val sqsClientConfig = SQSClientConfig(
    accessKey = awsSQSAccessKey.toOption,
    secretKey = awsSQSSecretKey.toOption,
    region = awsSQSRegion(),
    endpoint = awsSQSEndpoint.toOption
  )

  val sqsConfig = SQSConfig(
    sqsQueueUrl(),
    sqsWaitTimeSeconds() seconds,
    sqsMaxMessages(),
    sqsParallelism()
  )
}

case class S3ClientConfig(
                           accessKey: Option[String],
                           secretKey: Option[String],
                           endpoint: Option[String],
                           region: String
                         )

case class SQSClientConfig(
                           accessKey: Option[String],
                           secretKey: Option[String],
                           endpoint: Option[String],
                           region: String
                         )

case class CloudwatchClientConfig(
                                   endpoint: Option[String],
                                   region: String
                                 )

