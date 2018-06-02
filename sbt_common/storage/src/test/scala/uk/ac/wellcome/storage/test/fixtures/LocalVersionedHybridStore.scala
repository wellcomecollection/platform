package uk.ac.wellcome.storage.test.fixtures

import java.io.InputStream

import com.gu.scanamo._
import com.gu.scanamo.syntax._
import io.circe.{Decoder, Encoder}
import org.scalatest.Matchers
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3._
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{
  HybridRecord,
  VHSConfig,
  VersionedHybridStore
}
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

trait LocalVersionedHybridStore
    extends LocalDynamoDbVersioned
    with S3
    with JsonTestUtil
    with Matchers {

  val defaultGlobalS3Prefix = "testing"

  def vhsLocalFlags(bucket: Bucket,
                    table: Table,
                    globalS3Prefix: String = defaultGlobalS3Prefix) =
    Map(
      "aws.vhs.s3.bucketName" -> bucket.name,
      "aws.vhs.s3.globalPrefix" -> globalS3Prefix,
      "aws.vhs.dynamo.tableName" -> table.name
    ) ++ s3ClientLocalFlags ++ dynamoClientLocalFlags

  def withTypeVHS[T <: Id, M, R](
    bucket: Bucket,
    table: Table,
    globalS3Prefix: String = defaultGlobalS3Prefix)(
    testWith: TestWith[VersionedHybridStore[T, M, S3TypeStore[T]], R])(
    implicit encoder: Encoder[T],
    decoder: Decoder[T]): R = {
    val s3Config = S3Config(bucketName = bucket.name)
    val dynamoConfig = DynamoConfig(table = table.name, index = table.index)
    val vhsConfig = VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config,
      globalS3Prefix = globalS3Prefix
    )

    val s3ObjectStore = new S3TypeStore[T](
      s3Client = s3Client
    )

    val store = new VersionedHybridStore[T, M, S3TypeStore[T]](
      vhsConfig = vhsConfig,
      s3ObjectStore = s3ObjectStore,
      dynamoDbClient = dynamoDbClient
    )

    testWith(store)
  }

  def withStreamVHS[M, R](bucket: Bucket,
                          table: Table,
                          globalS3Prefix: String = defaultGlobalS3Prefix)(
    testWith: TestWith[VersionedHybridStore[InputStream, M, S3StreamStore], R])
    : R = {
    val s3Config = S3Config(bucketName = bucket.name)

    val dynamoConfig = DynamoConfig(table = table.name, index = table.index)

    val vhsConfig = VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config,
      globalS3Prefix = globalS3Prefix
    )

    val s3ObjectStore = new S3StreamStore(
      s3Client = s3Client
    )

    val store = new VersionedHybridStore[InputStream, M, S3StreamStore](
      vhsConfig = vhsConfig,
      s3ObjectStore = s3ObjectStore,
      dynamoDbClient = dynamoDbClient
    )

    testWith(store)
  }

  def withStringVHS[M, R](bucket: Bucket,
                          table: Table,
                          globalS3Prefix: String = defaultGlobalS3Prefix)(
    testWith: TestWith[VersionedHybridStore[String, M, S3StringStore], R])
    : R = {
    val s3Config = S3Config(bucketName = bucket.name)
    val dynamoConfig = DynamoConfig(table = table.name, index = table.index)
    val vhsConfig = VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config,
      globalS3Prefix = globalS3Prefix
    )

    val s3ObjectStore = new S3StringStore(
      s3Client = s3Client
    )

    val store = new VersionedHybridStore[String, M, S3StringStore](
      vhsConfig = vhsConfig,
      s3ObjectStore = s3ObjectStore,
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
    val hybridRecord = getHybridRecord(bucket, table, record.id)

    getJsonFromS3(bucket, hybridRecord.s3key).noSpaces
  }

  def getContentFor(bucket: Bucket, table: Table, id: String) = {
    val hybridRecord = getHybridRecord(bucket, table, id)

    getContentFromS3(bucket, hybridRecord.s3key)
  }

  private def getHybridRecord(bucket: Bucket, table: Table, id: String) =
    Scanamo.get[HybridRecord](dynamoDbClient)(table.name)('id -> id) match {
      case None => throw new RuntimeException(s"No object with id $id found!")
      case Some(read) =>
        read match {
          case Left(error) =>
            throw new RuntimeException(s"Error reading from dynamo: $error")
          case Right(record) => record
        }
    }
}
