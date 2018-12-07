package uk.ac.wellcome.messaging.test.fixtures

import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import io.circe.generic.extras.JsonKey
import io.circe.{yaml, Decoder, Json, ParsingFailure}
import org.scalatest.Matchers
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{
  SNSClientFactory,
  SNSConfig,
  SNSMessageWriter,
  SNSWriter
}
import uk.ac.wellcome.test.fixtures._

import scala.language.higherKinds
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Random, Success, Try}

object SNS {

  class Topic(val arn: String) extends AnyVal {
    override def toString = s"SNS.Topic($arn)"
  }

  object Topic {
    def apply(arn: String): Topic = new Topic(arn)
  }

}

trait SNS extends Matchers with Logging {

  import SNS._

  private val localSNSEndpointUrl = "http://localhost:9292"

  private val regionName = "localhost"

  private val accessKey = "access"
  private val secretKey = "secret"

  val snsClient: AmazonSNS = SNSClientFactory.create(
    region = regionName,
    endpoint = localSNSEndpointUrl,
    accessKey = accessKey,
    secretKey = secretKey
  )

  def withLocalSnsTopic[R]: Fixture[Topic, R] = fixture[Topic, R](
    create = {
      val topicName = Random.alphanumeric take 10 mkString
      val arn = snsClient.createTopic(topicName).getTopicArn
      Topic(arn)
    },
    destroy = { topic =>
      snsClient.deleteTopic(topic.arn)
    }
  )

  val localStackSnsClient: AmazonSNS = SNSClientFactory.create(
    region = "eu-west-2",
    endpoint = "http://localhost:4575",
    accessKey = accessKey,
    secretKey = secretKey
  )

  def withLocalStackSnsTopic[R]: Fixture[Topic, R] = fixture[Topic, R](
    create = {
      val topicName = Random.alphanumeric take 10 mkString
      val arn = localStackSnsClient.createTopic(topicName).getTopicArn
      Topic(arn)
    },
    destroy = { topic =>
      localStackSnsClient.deleteTopic(topic.arn)
    }
  )

  def withSNSMessageWriter[R](testWith: TestWith[SNSMessageWriter, R]): R =
    testWith(new SNSMessageWriter(snsClient = snsClient))

  def withSNSWriter[R](topic: Topic)(testWith: TestWith[SNSWriter, R]): R =
    withSNSMessageWriter { snsMessageWriter =>
      val snsWriter = new SNSWriter(
        snsMessageWriter = snsMessageWriter,
        snsConfig = SNSConfig(topic.arn)
      )
      testWith(snsWriter)
    }

  // For some reason, Circe struggles to decode MessageInfo if you use @JsonKey
  // to annotate the fields, and I don't care enough to work out why right now.
  implicit val messageInfoDecoder: Decoder[MessageInfo] =
    Decoder.forProduct4(":id", ":message", ":subject", ":topic_arn")(
      MessageInfo.apply)

  def listMessagesReceivedFromSNS(topic: Topic): Seq[MessageInfo] = {
    /*
    This is a sample returned by the fake-sns implementation:
    ---
    topics:
    - arn: arn:aws:sns:us-east-1:123456789012:es_ingest
      name: es_ingest
    - arn: arn:aws:sns:us-east-1:123456789012:id_minter
      name: id_minter
    messages:
    - :id: acbca1e1-e3c5-4c74-86af-06a9418e8fe4
      :subject: Foo
      :message: '{"identifiers":[{"source":"Miro","sourceId":"MiroID","value":"1234"}],"title":"some
        image title","accessStatus":null}'
      :topic_arn: arn:aws:sns:us-east-1:123456789012:id_minter
      :structure:
      :target_arn:
      :received_at: 2017-04-18 13:20:45.289912607 +00:00
     */
    val string = scala.io.Source.fromURL(localSNSEndpointUrl).mkString

    val json: Either[ParsingFailure, Json] = yaml.parser.parse(string)

    val snsResponse: SNSResponse = json.right.get
      .as[SNSResponse]
      .right
      .get

    snsResponse.messages
      .filter {
        _.topicArn == topic.arn
      }
  }

  def assertSnsReceivesOnly[T](expectedMessage: T, topic: SNS.Topic)(
    implicit decoderT: Decoder[T]) = {
    assertSnsReceives(Set(expectedMessage), topic)
  }

  def assertSnsReceivesNothing(topic: SNS.Topic) = {
    notificationCount(topic) shouldBe 0
  }

  def assertSnsReceives[T, I[T] <: Iterable[T]](
    expectedMessages: I[T],
    topic: SNS.Topic)(implicit decoderT: Decoder[T]) = {
    val triedReceiptsT = listNotifications[T](topic).toSet

    debug(s"SNS $topic received $triedReceiptsT")
    triedReceiptsT should have size expectedMessages.size

    val maybeT = triedReceiptsT collect {
      case Success(t) => t
    }

    maybeT should not be empty
    maybeT shouldBe expectedMessages.toSet
  }

  private def getMessages(topic: Topic) = {
    val string = scala.io.Source.fromURL(localSNSEndpointUrl).mkString
    val json = yaml.parser.parse(string)

    json.right
      .flatMap(_.as[SNSNotificationResponse])
      .right
      .map {
        _.messages.filter(_.topicArn == topic.arn)
      }
  }

  def notificationCount(topic: Topic): Int = {
    val messages = getMessages(topic)

    messages match {
      case Left(e)  => throw (e)
      case Right(t) => t.size
    }
  }

  def listNotifications[T](topic: Topic)(
    implicit decoderT: Decoder[T]): Seq[Try[T]] = {
    val messages = getMessages(topic)

    val eitherT = messages.right
      .map(_.map(m => fromJson[T](m.message)))

    eitherT match {
      case Left(e)  => throw e
      case Right(t) => t
    }
  }

  def notificationMessage[T](topic: Topic)(implicit decoderT: Decoder[T]): T = {
    notificationCount(topic) shouldBe 1
    val maybeT = listNotifications[T](topic).head
    maybeT.get
  }

  def createSNSConfigWith(topic: Topic): SNSConfig =
    SNSConfig(topicArn = topic.arn)
}

case class SNSNotificationMessage(
  @JsonKey(":id") id: String,
  @JsonKey(":subject") subject: Option[String],
  @JsonKey(":message") message: String,
  @JsonKey(":topic_arn") topicArn: String
)

case class SNSNotificationResponse(
  topics: List[TopicInfo],
  messages: List[SNSNotificationMessage]
)

case class SNSResponse(
  topics: List[TopicInfo],
  messages: List[MessageInfo] = Nil
)

case class TopicInfo(
  arn: String,
  name: String
)

case class MessageInfo(
  messageId: String,
  message: String,
  subject: String,
  topicArn: String
)
