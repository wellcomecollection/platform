package uk.ac.wellcome.platform.sierra_bib_merger

import io.circe.Encoder
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.SourceMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class SierraBibMergerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with MockitoSugar
    with ExtendedPatience
    with ScalaFutures
    with SQS
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

  implicit val encoder = Encoder[SierraTransformable]

  it("should store a bib in the hybrid store") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
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

              sendNotificationToSQS(queue = queue, message = record)

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = record)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraTransformable.id,
                  record = expectedSierraTransformable)
              }
            }
          }
        }
      }
    }
  }

  it("stores multiple bibs from SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
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

              sendNotificationToSQS(queue = queue, message = record1)

              val expectedSierraTransformable1 =
                SierraTransformable(bibRecord = record1)

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

              sendNotificationToSQS(queue = queue, message = record2)

              val expectedSierraTransformable2 =
                SierraTransformable(bibRecord = record2)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraTransformable1.id,
                  record = expectedSierraTransformable1)
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraTransformable2.id,
                  record = expectedSierraTransformable2)
              }
            }
          }
        }
      }
    }
  }

  it("updates a bib if a newer version is sent to SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
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
                .updateRecord(oldRecord.id)(
                  (oldRecord, SourceMetadata(oldRecord.sourceName)))((t, m) =>
                  (t, m))
                .map { _ =>
                  sendNotificationToSQS(queue = queue, message = record)
                }

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = record)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraTransformable.id,
                  record = expectedSierraTransformable)
              }
            }
          }
        }
      }
    }
  }

  it("does not update a bib if an older version is sent to SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
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
                .updateRecord(expectedSierraTransformable.id)((
                  expectedSierraTransformable,
                  SourceMetadata(expectedSierraTransformable.sourceName)))(
                  (t, m) => (t, m))
                .map { _ =>
                  sendNotificationToSQS(queue = queue, message = record)
                }

              // Blocking in Scala is generally a bad idea; we do it here so there's
              // enough time for this update to have gone through (if it was going to).
              Thread.sleep(5000)

              assertStored[SierraTransformable](
                bucket,
                table,
                id = expectedSierraTransformable.id,
                record = expectedSierraTransformable)
            }
          }
        }
      }
    }
  }

  it("stores a bib from SQS if the ID already exists but no bibData") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Unit](
              bucket,
              table) { hybridStore =>
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
                hybridStore.updateRecord(newRecord.id)(
                  (newRecord, SourceMetadata(newRecord.sourceName)))((t, m) =>
                  (t, m))

              future.map { _ =>
                sendNotificationToSQS(queue = queue, message = record)
              }

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = record)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraTransformable.id,
                  record = expectedSierraTransformable)
              }
            }
          }
        }
      }
    }
  }
}
