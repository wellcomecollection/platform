package uk.ac.wellcome.storage

import com.gu.scanamo.syntax._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.Suite
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.models.{Id, Sourced}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil, S3Local}

trait VersionedHybridStoreLocal[T <: Id]
    extends DynamoDBLocal[HybridRecord]
    with S3Local
    with JsonTestUtil
    with ExtendedPatience { this: Suite =>

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  val keyPrefixGenerator: KeyPrefixGenerator[T]

  val hybridStore = new VersionedHybridStore[T](
    sourcedObjectStore = new S3ObjectStore(
      s3Client = s3Client,
      bucketName = bucketName,
      keyPrefixGenerator = keyPrefixGenerator
    ),
    versionedDao = new VersionedDao(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(
        table = tableName
      ))
  )

  def assertHybridRecordIsStoredCorrectly(record: T, expectedJson: String) = {
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
