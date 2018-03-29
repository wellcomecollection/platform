package uk.ac.wellcome.test.fixtures

import com.gu.scanamo._
import com.gu.scanamo.syntax._
import org.scalatest.Assertion
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.storage.{HybridRecord, VersionedHybridStore}
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}

trait LocalVersionedHybridStore extends LocalDynamoDb[HybridRecord] with S3 {

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  def withVersionedDao[R](tableName: String)(
    testWith: TestWith[VersionedDao, R]): R = {
    val dao = new VersionedDao(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(tableName)
    )
    testWith(dao)
  }

  def withVersionedHybridStore[T <: Id, R](
    bucketName: String,
    tableName: String)(testWith: TestWith[VersionedHybridStore[T], R]): R = {
    withVersionedDao(tableName) { dao =>
      val store = new VersionedHybridStore[T](
        sourcedObjectStore = new S3ObjectStore(
          s3Client = s3Client,
          bucketName = bucketName,
          keyPrefixGenerator = new KeyPrefixGenerator[T] {
            override def generate(obj: T): String = "/"
          }
        ),
        versionedDao = dao
      )
      testWith(store)
    }
  }

  def getJsonFor[T <: Id](bucketName: String, tableName: String, record: T) = {
    val hybridRecord = Scanamo
      .get[HybridRecord](dynamoDbClient)(tableName)('id -> record.id)
      .get
      .right
      .get

    getJsonFromS3(bucketName, hybridRecord.s3key).noSpaces
  }
}
