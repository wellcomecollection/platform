package uk.ac.wellcome.test.fixtures

import com.gu.scanamo._
import com.gu.scanamo.syntax._
import io.circe.Encoder
import org.scalatest.{Assertion, Matchers}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.models.aws.{DynamoConfig, S3Config}
import uk.ac.wellcome.storage.{HybridRecord, VersionedHybridStore}
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

trait LocalVersionedHybridStore
    extends LocalDynamoDb[HybridRecord]
    with S3
    with JsonTestUtil
    with Matchers {

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  def withVersionedDao(tableName: String)(
    testWith: TestWith[VersionedDao, Assertion]) {
    val dao = new VersionedDao(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(tableName)
    )
    testWith(dao)
  }

  def withVersionedHybridStore[T <: Id](bucketName: String, tableName: String)(
    testWith: TestWith[VersionedHybridStore[T], Assertion]) = {
    withVersionedDao(tableName) { dao =>
      val store = new VersionedHybridStore[T](
        sourcedObjectStore = new S3ObjectStore(
          s3Client = s3Client,
          s3Config = S3Config(bucketName = bucketName),
          keyPrefixGenerator = new KeyPrefixGenerator[T] {
            override def generate(obj: T): String = "/"
          }
        ),
        versionedDao = dao
      )
      testWith(store)
    }
  }

  def assertStored[T <: Id](bucketName: String, tableName: String, record: T)(
    implicit encoder: Encoder[T]) =
    assertJsonStringsAreEqual(
      getJsonFor[T](bucketName, tableName, record),
      toJson(record).get
    )

  def getJsonFor[T <: Id](bucketName: String, tableName: String, record: T) = {
    val hybridRecord = Scanamo
      .get[HybridRecord](dynamoDbClient)(tableName)('id -> record.id)
      .get
      .right
      .get

    getJsonFromS3(bucketName, hybridRecord.s3key).noSpaces
  }
}
