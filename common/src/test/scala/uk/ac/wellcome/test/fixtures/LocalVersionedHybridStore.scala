package uk.ac.wellcome.test.fixtures

import uk.ac.wellcome.dynamo.VersionedDao
import com.gu.scanamo._
import com.gu.scanamo.syntax._
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.storage.VersionedHybridStore
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}

trait LocalVersionedHybridStore extends LocalDynamoDb[HybridRecord] with S3 {

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  def withVersionedDao[R](testWith: TestWith[(VersionedDao, String), R]) {
    withLocalDynamoDbTable { tableName =>
      val config = DynamoConfig(tableName)
      val dao = new VersionedDao(dynamoDbClient, config)
      testWith((dao, tableName))
    }
  }

  def withVersionedHybridStore[T <: Id, R](bucketName: String)(
    testWith: TestWith[(VersionedHybridStore[T], String), R]) = {
    withVersionedDao {
      case (dao, tableName) =>
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

        testWith((store, tableName))
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
