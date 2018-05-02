package uk.ac.wellcome.storage.test.fixtures

import com.gu.scanamo._
import com.gu.scanamo.syntax._
import io.circe.Encoder
import org.scalatest.{Assertion, Matchers}
import uk.ac.wellcome.models.aws.{DynamoConfig, S3Config}
import uk.ac.wellcome.storage.vhs.{HybridRecord, VersionedHybridStore}
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.storage.dynamo.VersionedDao
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures.S3.Bucket

trait LocalVersionedHybridStore
    extends LocalDynamoDb[HybridRecord]
    with S3
    with JsonTestUtil
    with Matchers
    with ImplicitLogging {

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  def withVersionedDao[R](table: Table) = fixture[VersionedDao, R](
    create = new VersionedDao(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(table.name)
    )
  )

  def withVersionedHybridStore[T <: Id, R](bucket: Bucket, table: Table)(
    testWith: TestWith[VersionedHybridStore[T], R]): R = {
    withVersionedDao(table) { dao =>
      val s3Config = S3Config(bucketName = bucket.name)
      val store = new VersionedHybridStore[T](
        s3Config = s3Config,
        sourcedObjectStore = new S3ObjectStore(
          s3Client = s3Client,
          s3Config = s3Config,
          keyPrefixGenerator = new KeyPrefixGenerator[T] {
            override def generate(obj: T): String = "/"
          }
        ),
        versionedDao = dao
      )
      testWith(store)
    }
  }

  def assertStored[T <: Id](bucket: Bucket, table: Table, record: T)(
    implicit encoder: Encoder[T]) =
    assertJsonStringsAreEqual(
      getJsonFor[T](bucket, table, record),
      toJson(record).get
    )

  def getJsonFor[T <: Id](bucket: Bucket, table: Table, record: T) = {
    val hybridRecord = Scanamo
      .get[HybridRecord](dynamoDbClient)(table.name)('id -> record.id)
      .get
      .right
      .get

    getJsonFromS3(bucket, hybridRecord.s3key).noSpaces
  }
}
