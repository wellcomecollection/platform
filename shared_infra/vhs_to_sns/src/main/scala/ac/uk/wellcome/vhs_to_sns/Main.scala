package ac.uk.wellcome.vhs_to_sns

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.gu.scanamo.DynamoFormat
import io.circe.syntax._
import io.circe.generic.auto._

import scala.collection.JavaConversions._

class Main {

  val localEndpointConfig: Option[LocalEndpointConfig] = for {
    endpoint <- sys.env.get("endpoint")
    accessKey <- sys.env.get("accessKey")
    secretKey <- sys.env.get("secretKey")
  } yield LocalEndpointConfig(endpoint, accessKey, secretKey)

  val awsConfig: Option[AWSConfig] = for {
    region <- sys.env.get("region")
  } yield AWSConfig(region)

  val config = (localEndpointConfig, awsConfig) match {
    case (Some(localEndpointConfig), _) => Left(localEndpointConfig)
    case (None, Some(awsConfig)) => Right(awsConfig)
    case _ => throw new Exception("NOT WORK BAD")
  }

  val snsClient = buildClient(config)

  def toMessagePointer(event: DynamodbEvent) = {
    val dynamoFormat = implicitly[DynamoFormat[HybridRecord]]

    val buffer = event.getRecords.map(
      dynamoDbStreamRecord =>
        dynamoFormat
          .read(new AttributeValue().withM(
            dynamoDbStreamRecord.getDynamodb.getNewImage))
          .right
          .get)

    buffer.foreach(hybridRecord => {
      val messagePointer = MessagePointer(
        S3ObjectLocation(sys.env.get("BUCKET_NAME").get, hybridRecord.s3key))
      snsClient.publish(
        sys.env.get("TOPIC_ARN").get,
        messagePointer.asJson.noSpaces
      )
    })
  }

  private def buildClient(config: Either[LocalEndpointConfig, AWSConfig]) = {
    val standardSnsClient = AmazonSNSClientBuilder.standard
    config match {
      case Right(awsConfig) =>
        standardSnsClient
          .withRegion(awsConfig.region)
          .build()
      case Left(localEndpointConfig) =>
        standardSnsClient
          .withCredentials(
            new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(
                localEndpointConfig.accessKey,
                localEndpointConfig.secretKey)))
          .withEndpointConfiguration(new EndpointConfiguration(
            localEndpointConfig.endpoint,
            "localhost"))
          .build()
    }

  }
}

case class LocalEndpointConfig(endpoint: String,
                               accessKey: String,
                               secretKey: String)
case class MessagePointer(src: S3ObjectLocation)
case class HybridRecord(
  id: String,
  version: Int,
  s3key: String
)
case class S3ObjectLocation(bucket: String, key: String)
case class AWSConfig(
  region: String
)
