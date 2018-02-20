package uk.ac.wellcome.platform.sierra_bib_merger

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.ActorSystem
import com.gu.scanamo.syntax._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.FunSpec
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  ExtendedPatience,
  SQSLocal
}

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord

import uk.ac.wellcome.storage.VersionedHybridStoreLocal

import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.dynamo._

class SierraBibMergerFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with AmazonCloudWatchFlag
    with SQSLocal
    with Eventually
    with MockitoSugar
    with ExtendedPatience
    with ScalaFutures
    with VersionedHybridStoreLocal {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  override lazy val tableName = "sierra-bib-merger-feature-test-table"
  override lazy val bucketName = "sierra-bib-merger-feature-test-bucket"
  val queueUrl = createQueueAndReturnUrl("test_bib_merger")

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.s3.bucketName" -> bucketName,
      "aws.dynamo.tableName" -> tableName
    ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags ++ s3LocalFlags
  )

  def bibRecordString(id: String,
                      updatedDate: String,
                      title: String = "Lehrbuch und Atlas der Gastroskopie") =
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
      |      "title": "$title",
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

  it("should store a bib in the hybrid store") {
    val id = "1000001"
    val record = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = "2001-01-01T01:01:01Z",
        title = "One ocelot on our oval"
      ),
      modifiedDate = "2001-01-01T01:01:01Z"
    )

    sendBibRecordToSQS(record)

    val expectedSierraTransformable = SierraTransformable(bibRecord = record)

    assertStored(expectedSierraTransformable)
  }

  it("stores multiple bibs from SQS") {
    val id1 = "1000001"
    val record1 = SierraBibRecord(
      id = id1,
      data = bibRecordString(
        id = id1,
        updatedDate = "2001-01-01T01:01:01Z",
        title = "The first ferret of four"
      ),
      modifiedDate = "2001-01-01T01:01:01Z"
    )
    sendBibRecordToSQS(record1)
    val expectedSierraTransformable1 = SierraTransformable(bibRecord = record1)

    val id2 = "2000002"
    val record2 = SierraBibRecord(
      id = id2,
      data = bibRecordString(
        id = id2,
        updatedDate = "2002-02-02T02:02:02Z",
        title = "The second swan of a set"
      ),
      modifiedDate = "2002-02-02T02:02:02Z"
    )
    sendBibRecordToSQS(record2)
    val expectedSierraTransformable2 = SierraTransformable(bibRecord = record2)

    assertStored(expectedSierraTransformable1)
    assertStored(expectedSierraTransformable2)
  }

  it("updates a bib if a newer version is sent to SQS") {
    val id = "3000003"
    val oldBibRecord = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = "2003-03-03T03:03:03Z",
        title = "Old orangutans outside an office"
      ),
      modifiedDate = "2003-03-03T03:03:03Z"
    )

    val oldRecord = SierraTransformable(bibRecord = oldBibRecord)

    val newTitle = "A number of new narwhals near Newmarket"
    val newUpdatedDate = "2004-04-04T04:04:04Z"
    val record = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = newUpdatedDate,
        title = newTitle
      ),
      modifiedDate = newUpdatedDate
    )

    hybridStore
      .updateRecord[SierraTransformable](
        oldRecord.sourceName,
        oldRecord.sourceId)(oldRecord)(identity)
      .map { _ =>
        sendBibRecordToSQS(record)
      }

    val expectedSierraTransformable = SierraTransformable(bibRecord = record)

    assertStored(expectedSierraTransformable)
  }

  it("does not update a bib if an older version is sent to SQS") {
    val id = "6000006"
    val newBibRecord = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = "2006-06-06T06:06:06Z",
        title = "A presence of pristine porpoises"
      ),
      modifiedDate = "2006-06-06T06:06:06Z"
    )

    val expectedSierraTransformable = SierraTransformable(bibRecord = newBibRecord)

    val oldTitle = "A small selection of sad shellfish"
    val oldUpdatedDate = "2001-01-01T01:01:01Z"
    val record = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = oldUpdatedDate,
        title = oldTitle
      ),
      modifiedDate = oldUpdatedDate
    )

    hybridStore
      .updateRecord[SierraTransformable](
        expectedSierraTransformable.sourceName,
        expectedSierraTransformable.sourceId)(expectedSierraTransformable)(
        identity)
      .map { _ =>
        sendBibRecordToSQS(record)
      }

    // Blocking in Scala is generally a bad idea; we do it here so there's
    // enough time for this update to have gone through (if it was going to).
    Thread.sleep(5000)

    assertStored(expectedSierraTransformable)
  }

  it("stores a bib from SQS if the ID already exists but no bibData") {
    val id = "7000007"
    val newRecord = SierraTransformable(sourceId = id)

    val title = "Inside an inquisitive igloo of ice imps"
    val updatedDate = "2007-07-07T07:07:07Z"
    val record = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        title = title,
        updatedDate = updatedDate
      ),
      modifiedDate = updatedDate
    )

    val future = hybridStore.updateRecord[SierraTransformable](
      newRecord.sourceName,
      newRecord.sourceId)(newRecord)(identity)

    future.map { _ =>
      sendBibRecordToSQS(record)
    }

    val expectedSierraTransformable = SierraTransformable(bibRecord = record)

    assertStored(expectedSierraTransformable)
  }

  private def assertStored(expectedRecord: SierraTransformable) = eventually {
    val actualRecord =
      Await
        .result(
          hybridStore
            .getRecord[SierraTransformable](expectedRecord.id),
          5 seconds
        )
        .get

    actualRecord shouldBe expectedRecord
  }

  private def sendBibRecordToSQS(record: SierraBibRecord) = {
    val messageBody = toJson(record).get

    val message = SQSMessage(
      subject = Some("Test message sent by SierraBibMergerWorkerServiceTest"),
      body = messageBody,
      topic = "topic",
      messageType = "messageType",
      timestamp = "2001-01-01T01:01:01Z"
    )
    sqsClient.sendMessage(queueUrl, toJson(message).get)
  }
}
