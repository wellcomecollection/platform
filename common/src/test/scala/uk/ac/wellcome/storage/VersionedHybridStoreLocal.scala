package uk.ac.wellcome.storage

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.Suite
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil, S3Local}

trait VersionedHybridStoreLocal
    extends DynamoDBLocal[HybridRecord]
    with S3Local
    with JsonTestUtil
    with ExtendedPatience { this: Suite =>

  def createHybridStore[T <: Id] = new VersionedHybridStore[T](
    sourcedObjectStore = new S3ObjectStore(
      s3Client = s3Client,
      bucketName = bucketName,
      keyPrefixGenerator = new KeyPrefixGenerator[T] {
        override def generate(obj: T): String = "/"
      }
    ),
    versionedDao = new VersionedDao(dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(
        table = tableName
      ))
  )

  def assertHybridRecordIsStoredCorrectly[T <: Id](record: T, expectedJson: String) = {
    val dynamoRecord = Scanamo
      .get[HybridRecord](dynamoDbClient)(tableName)('id -> record.id)
      .get
    dynamoRecord.isRight shouldBe true
    val hybridRecord = dynamoRecord.right.get

    val s3key = hybridRecord.s3key

    val retrievedJson =
      getJsonFromS3(bucketName = bucketName, key = s3key).noSpaces
    assertJsonStringsAreEqual(retrievedJson, expectedJson) // toJson(record.copy(version = record.version + 1)).get)
  }
}
