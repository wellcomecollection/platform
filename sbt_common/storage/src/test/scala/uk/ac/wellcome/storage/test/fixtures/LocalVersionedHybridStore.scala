package uk.ac.wellcome.storage.test.fixtures

import com.gu.scanamo._
import com.gu.scanamo.syntax._
import io.circe.Encoder
import org.scalatest.{Assertion, Matchers}
import uk.ac.wellcome.storage.vhs.{HybridRecord, VersionedHybridStore}
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.storage.dynamo.{DynamoConfig, VersionedDao}
import uk.ac.wellcome.storage.s3.{KeyPrefixGenerator, S3Config, S3ObjectStore}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.VHSConfig
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

trait LocalVersionedHybridStore
    extends LocalDynamoDb[HybridRecord]
    with S3
    with JsonTestUtil
    with Matchers
    with ImplicitLogging {

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  def vhsLocalFlags(bucket: Bucket, table: Table) =
    Map(
      "aws.vhs.s3.bucketName" -> bucket.name,
      "aws.vhs.dynamo.tableName" -> table.name
    ) ++ s3ClientLocalFlags ++ dynamoClientLocalFlags

  def withVersionedHybridStore[T <: Id, R](bucket: Bucket, table: Table)(
    testWith: TestWith[VersionedHybridStore[T], R]): R = {
    val s3Config = S3Config(bucketName = bucket.name)
    val dynamoConfig = DynamoConfig(table = table.name)
    val vhsConfig = VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config
    )

    val store = new VersionedHybridStore[T](
      vhsConfig = vhsConfig,
      s3Client = s3Client,
      keyPrefixGenerator = new KeyPrefixGenerator[T] {
        override def generate(obj: T): String = "/"
      },
      dynamoDbClient = dynamoDbClient
    )

    testWith(store)
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
