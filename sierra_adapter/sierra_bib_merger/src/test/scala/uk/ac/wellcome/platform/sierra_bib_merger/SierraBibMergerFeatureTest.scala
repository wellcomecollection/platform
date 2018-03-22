package uk.ac.wellcome.platform.sierra_bib_merger

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.SourceMetadata
import uk.ac.wellcome.test.fixtures.{LocalVersionedHybridStore, SqsFixtures}

import scala.concurrent.ExecutionContext.Implicits.global

class SierraBibMergerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with MockitoSugar
    with ExtendedPatience
    with ScalaFutures
    with JsonTestUtil
    with SqsFixtures
    with fixtures.Server
    with LocalVersionedHybridStore {

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
    withLocalSqsQueue { queueUrl =>
      withLocalS3Bucket { bucketName =>
        withLocalDynamoDbTable { tableName =>
          val flags = sqsLocalFlags(queueUrl) ++ s3LocalFlags(bucketName) ++ dynamoDbLocalEndpointFlags(tableName)
          withServer(flags) { _ =>
            withVersionedHybridStore[SierraTransformable](bucketName, tableName) { hybridStore =>
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

              sendMessageToSQS(toJson(record).get, queueUrl = queueUrl)

              val expectedSierraTransformable = SierraTransformable(bibRecord = record)

              assertJsonStringsAreEqual(
                getJsonFor[SierraTransformable](bucketName, tableName, expectedSierraTransformable),
                toJson(expectedSierraTransformable).get
              )
            }
          }
        }
      }
    }
  }

  it("stores multiple bibs from SQS") {
    withLocalSqsQueue { queueUrl =>
      withLocalS3Bucket { bucketName =>
        withLocalDynamoDbTable { tableName =>
          val flags = sqsLocalFlags(queueUrl) ++ s3LocalFlags(bucketName) ++ dynamoDbLocalEndpointFlags(tableName)
          withServer(flags) { _ =>
            withVersionedHybridStore[SierraTransformable](bucketName, tableName) { hybridStore =>
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

              sendMessageToSQS(toJson(record1).get, queueUrl = queueUrl)

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

              sendMessageToSQS(toJson(record2).get, queueUrl = queueUrl)

              val expectedSierraTransformable2 = SierraTransformable(bibRecord = record2)

              assertJsonStringsAreEqual(
                getJsonFor[SierraTransformable](bucketName, tableName, expectedSierraTransformable1),
                toJson(expectedSierraTransformable1).get
              )
              assertJsonStringsAreEqual(
                getJsonFor[SierraTransformable](bucketName, tableName, expectedSierraTransformable2),
                toJson(expectedSierraTransformable2).get
              )
            }
          }
        }
      }
    }
  }

  it("updates a bib if a newer version is sent to SQS") {
    withLocalSqsQueue { queueUrl =>
      withLocalS3Bucket { bucketName =>
        withLocalDynamoDbTable { tableName =>
          val flags = sqsLocalFlags(queueUrl) ++ s3LocalFlags(bucketName) ++ dynamoDbLocalEndpointFlags(tableName)
          withServer(flags) { _ =>
            withVersionedHybridStore[SierraTransformable](bucketName, tableName) { hybridStore =>
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
                .updateRecord(oldRecord.id)(oldRecord)(identity)(
                  SourceMetadata(oldRecord.sourceName))
                .map { _ =>
                  sendMessageToSQS(toJson(record).get, queueUrl = queueUrl)
                }

              val expectedSierraTransformable = SierraTransformable(bibRecord = record)

              assertJsonStringsAreEqual(
                getJsonFor[SierraTransformable](bucketName, tableName, expectedSierraTransformable),
                toJson(expectedSierraTransformable).get
              )
            }
          }
        }
      }
    }
  }

  it("does not update a bib if an older version is sent to SQS") {
    withLocalSqsQueue { queueUrl =>
      withLocalS3Bucket { bucketName =>
        withLocalDynamoDbTable { tableName =>
          val flags = sqsLocalFlags(queueUrl) ++ s3LocalFlags(bucketName) ++ dynamoDbLocalEndpointFlags(tableName)
          withServer(flags) { _ =>
            withVersionedHybridStore[SierraTransformable](bucketName, tableName) { hybridStore =>
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

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = newBibRecord)

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
                .updateRecord(expectedSierraTransformable.id)(
                  expectedSierraTransformable)(identity)(
                  SourceMetadata(expectedSierraTransformable.sourceName))
                .map { _ =>
                  sendMessageToSQS(toJson(record).get, queueUrl = queueUrl)
                }

              // Blocking in Scala is generally a bad idea; we do it here so there's
              // enough time for this update to have gone through (if it was going to).
              Thread.sleep(5000)

              assertJsonStringsAreEqual(
                getJsonFor[SierraTransformable](bucketName, tableName, expectedSierraTransformable),
                toJson(expectedSierraTransformable).get
              )
            }
          }
        }
      }
    }
  }

  it("stores a bib from SQS if the ID already exists but no bibData") {
    withLocalSqsQueue { queueUrl =>
      withLocalS3Bucket { bucketName =>
        withLocalDynamoDbTable { tableName =>
          val flags = sqsLocalFlags(queueUrl) ++ s3LocalFlags(bucketName) ++ dynamoDbLocalEndpointFlags(tableName)
          withServer(flags) { _ =>
            withVersionedHybridStore[SierraTransformable](bucketName, tableName) { hybridStore =>
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

              val future =
                hybridStore.updateRecord(newRecord.id)(newRecord)(identity)(
                  SourceMetadata(newRecord.sourceName))

              future.map { _ =>
                sendMessageToSQS(toJson(record).get, queueUrl = queueUrl)
              }

              val expectedSierraTransformable = SierraTransformable(bibRecord = record)

              assertJsonStringsAreEqual(
                getJsonFor[SierraTransformable](bucketName, tableName, expectedSierraTransformable),
                toJson(expectedSierraTransformable).get
              )
            }
          }
        }
      }
    }
  }

  private def sendMessageToSQS(message: String, queueUrl: String) = {
    val message = SQSMessage(
      subject = Some("Test message sent by SierraBibMergerWorkerServiceTest"),
      body = message,
      topic = "topic",
      messageType = "messageType",
      timestamp = "2001-01-01T01:01:01Z"
    )
    sqsClient.sendMessage(queueUrl, toJson(message).get)
  }
}
