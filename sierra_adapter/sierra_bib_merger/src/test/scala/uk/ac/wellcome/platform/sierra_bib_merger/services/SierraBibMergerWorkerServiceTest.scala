package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.SierraRecord
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_bib_merger.Server
import uk.ac.wellcome.platform.sierra_bib_merger.locals.DynamoDBLocal
import uk.ac.wellcome.platform.sierra_bib_merger.models.MergedSierraObject
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil


class SierraBibMergerWorkerServiceTest
    extends FunSpec
    with FeatureTestMixin
    with AmazonCloudWatchFlag
    with Matchers
    with SQSLocal
    with DynamoDBLocal {

  val queueUrl = createQueueAndReturnUrl("test_bib_merger")

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.dynamo.sierraBibMerger.tableName" -> tableName
    ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
  )

  def bibRecordString(id: String, updatedDate: String) =
    s"""
      |{
      |      "id": "$id",
      |      "updatedDate": "$updatedDate",
      |      "createdDate": "1999-11-01T16:36:51Z",
      |      "deleted": false,
      |      "suppressed": false,
      |      "lang": {
      |        "code": "ger",
      |        "name": "German"
      |      },
      |      "title": "Lehrbuch und Atlas der Gastroskopie",
      |      "author": "Schindler, Rudolf, 1888-",
      |      "materialType": {
      |        "code": "a",
      |        "value": "Books"
      |      },
      |      "bibLevel": {
      |        "code": "m",
      |        "value": "MONOGRAPH"
      |      },
      |      "publishYear": 1923,
      |      "catalogDate": "1999-01-01",
      |      "country": {
      |        "code": "gw ",
      |        "name": "Germany"
      |      }
      |    }
    """.stripMargin

  def generateSierraRecordMessageBody(id: String, updatedDate: String): String = {
    val record = SierraRecord(
      id = id,
      data = bibRecordString(id, updatedDate),
      modifiedDate = updatedDate
    )

    JsonUtil.toJson(record).get
  }

  it("should put a bib from SQS into Dynamo") {
    val id = "1000017"
    val updatedDate = "2013-12-13T12:43:16Z"

    val messageBody = generateSierraRecordMessageBody(id, updatedDate)

    val message = SQSMessage(
      subject = None,
      body = messageBody,
      topic = "topic",
      messageType = "messageType",
      timestamp = "timestamp"
    )

    val response = sqsClient.sendMessage(queueUrl, JsonUtil.toJson(message).get)
    println(s"@@AWLC response = $response")

    val expectedMergedSierraObject = MergedSierraObject(id)

    eventually {
      val actualMergedSierraObject =
        Scanamo.get[MergedSierraObject](dynamoDbClient)(tableName)(
          'id -> id
        ).get

      actualMergedSierraObject shouldEqual Right(expectedMergedSierraObject)
    }
  }
}
