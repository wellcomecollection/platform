package uk.ac.wellcome.platform.sierra_bib_merger

import akka.actor.ActorSystem
import com.gu.scanamo.syntax._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  ExtendedPatience,
  SQSLocal
}
import uk.ac.wellcome.sierra_adapter.utils.SierraTestUtils
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.VersionUpdater
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.storage.VersionedHybridStoreLocal
import uk.ac.wellcome.utils.JsonUtil._

class SierraBibMergerFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with AmazonCloudWatchFlag
    with SQSLocal
    with SierraTestUtils
    with ExtendedPatience
    with ScalaFutures
    with VersionedHybridStoreLocal {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  implicit val sierraTransformableUpdater =
    new VersionUpdater[SierraTransformable] {
      override def updateVersion(sierraTransformable: SierraTransformable,
                                 newVersion: Int): SierraTransformable = {
        sierraTransformable.copy(version = newVersion)
      }
    }

  override lazy val tableName = "sierra-bib-merger-feature-test-table"
  override lazy val bucketName = "sierra-bib-merger-feature-test-bucket"
  val queueUrl = createQueueAndReturnUrl("test_bib_merger")

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.s3.bucketName" -> bucketName,
      "aws.dynamo.dynamoTable.tableName" -> tableName
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

    val expectedSierraTransformable =
      SierraTransformable(bibRecord = record, version = 1)

    eventually {
      val futureRecord = hybridStore.getRecord[SierraTransformable](expectedSierraTransformable.id)
      whenReady(futureRecord) { record =>
        record.get shouldBe expectedSierraTransformable
      }
    }
  }

//  it("should put multiple bibs from SQS into DynamoDB") {
//    val id1 = "1000001"
//    val record1 = SierraBibRecord(
//      id = id1,
//      data = bibRecordString(
//        id = id1,
//        updatedDate = "2001-01-01T01:01:01Z",
//        title = "The first ferret of four"
//      ),
//      modifiedDate = "2001-01-01T01:01:01Z"
//    )
//    sendBibRecordToSQS(record1)
//    val expectedSierraTransformable1 =
//      SierraTransformable(bibRecord = record1, version = 1)
//
//    val id2 = "2000002"
//    val record2 = SierraBibRecord(
//      id = id2,
//      data = bibRecordString(
//        id = id2,
//        updatedDate = "2002-02-02T02:02:02Z",
//        title = "The second swan of a set"
//      ),
//      modifiedDate = "2002-02-02T02:02:02Z"
//    )
//    sendBibRecordToSQS(record2)
//    val expectedSierraTransformable2 =
//      SierraTransformable(bibRecord = record2, version = 1)
//
//    dynamoQueryEqualsValue('id -> id1)(
//      expectedValue = expectedSierraTransformable1)
//    dynamoQueryEqualsValue('id -> id2)(
//      expectedValue = expectedSierraTransformable2)
//  }
//
//  it("should update a bib in DynamoDB if a newer version is sent to SQS") {
//    val id = "3000003"
//    val oldBibRecord = SierraBibRecord(
//      id = id,
//      data = bibRecordString(
//        id = id,
//        updatedDate = "2003-03-03T03:03:03Z",
//        title = "Old orangutans outside an office"
//      ),
//      modifiedDate = "2003-03-03T03:03:03Z"
//    )
//    val oldRecord = SierraTransformable(bibRecord = oldBibRecord, version = 1)
//    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)
//
//    val newTitle = "A number of new narwhals near Newmarket"
//    val newUpdatedDate = "2004-04-04T04:04:04Z"
//    val record = SierraBibRecord(
//      id = id,
//      data = bibRecordString(
//        id = id,
//        updatedDate = newUpdatedDate,
//        title = newTitle
//      ),
//      modifiedDate = newUpdatedDate
//    )
//    sendBibRecordToSQS(record)
//
//    val expectedSierraRecord =
//      SierraTransformable(bibRecord = record, version = 2)
//    dynamoQueryEqualsValue('id -> id)(expectedValue = expectedSierraRecord)
//  }
//
//  it("should not update a bib in DynamoDB if an older version is sent to SQS") {
//    val id = "6000006"
//    val newBibRecord = SierraBibRecord(
//      id = id,
//      data = bibRecordString(
//        id = id,
//        updatedDate = "2006-06-06T06:06:06Z",
//        title = "A presence of pristine porpoises"
//      ),
//      modifiedDate = "2006-06-06T06:06:06Z"
//    )
//    val newRecord = SierraTransformable(bibRecord = newBibRecord)
//    Scanamo.put(dynamoDbClient)(tableName)(newRecord)
//    val expectedSierraRecord = SierraTransformable(bibRecord = newBibRecord)
//
//    val oldTitle = "A small selection of sad shellfish"
//    val oldUpdatedDate = "2001-01-01T01:01:01Z"
//    val record = SierraBibRecord(
//      id = id,
//      data = bibRecordString(
//        id = id,
//        updatedDate = oldUpdatedDate,
//        title = oldTitle
//      ),
//      modifiedDate = oldUpdatedDate
//    )
//    sendBibRecordToSQS(record)
//
//    // Blocking in Scala is generally a bad idea; we do it here so there's
//    // enough time for this update to have gone through (if it was going to).
//    Thread.sleep(5000)
//
////    dynamoQueryEqualsValue('id -> id)(expectedValue = expectedSierraRecord)
//  }
//
//  it("stores a bib from SQS if the ID already exists but no bibData") {
//    val id = "7000007"
//    val newRecord = SierraTransformable(sourceId = id, version = 1)
//
//    val title = "Inside an inquisitive igloo of ice imps"
//    val updatedDate = "2007-07-07T07:07:07Z"
//    val record = SierraBibRecord(
//      id = id,
//      data = bibRecordString(
//        id = id,
//        title = title,
//        updatedDate = updatedDate
//      ),
//      modifiedDate = updatedDate
//    )
//
//    val future = hybridStore.updateRecord[SierraTransformable](newRecord)
//
//    val expectedRecord = SierraTransformable(bibRecord = record, version = 2)
//
//    future.map { _ =>
//      sendBibRecordToSQS(record)
//    }
//
//    eventually {
//      hybridStore.getRecord[SierraTransformable](expectedRecord.sourceId) shouldBe expectedRecord.copy(version = 3)
//    }
//  }

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
