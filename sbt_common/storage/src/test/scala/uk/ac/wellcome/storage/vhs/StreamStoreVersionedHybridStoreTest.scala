package uk.ac.wellcome.storage.vhs

import java.io.{ByteArrayInputStream, InputStream}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class StreamStoreVersionedHybridStoreTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with LocalVersionedHybridStore {

  import uk.ac.wellcome.storage.dynamo._

  val emptyMetadata = EmptyMetadata()

  private def stringify(is: InputStream) =
    scala.io.Source.fromInputStream(is).mkString

  def withStreamVHS[Metadata, R](
    bucket: Bucket,
    table: Table,
    globalS3Prefix: String = defaultGlobalS3Prefix)(
    testWith: TestWith[
      VersionedHybridStore[InputStream, Metadata, ObjectStore[InputStream]],
      R])(
    implicit objectStore: ObjectStore[InputStream]
  ): R = {
    val s3Config = S3Config(bucketName = bucket.name)

    val dynamoConfig =
      DynamoConfig(table = table.name, maybeIndex = Some(table.index))

    val vhsConfig = VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config,
      globalS3Prefix = globalS3Prefix
    )

    val store = new VersionedHybridStore[
      InputStream,
      Metadata,
      ObjectStore[InputStream]](
      vhsConfig = vhsConfig,
      objectStore = objectStore,
      dynamoDbClient = dynamoDbClient
    )

    testWith(store)
  }

  def withS3StreamStoreFixtures[R](
    testWith: TestWith[(Bucket,
                        Table,
                        VersionedHybridStore[InputStream,
                                             EmptyMetadata,
                                             ObjectStore[InputStream]]),
                       R]): R =
    withLocalS3Bucket[R] { bucket =>
      withLocalDynamoDbTable[R] { table =>
        withStreamVHS[EmptyMetadata, R](bucket, table) { vhs =>
          testWith((bucket, table, vhs))
        }
      }
    }

  describe("with S3StreamStore") {
    it("stores an InputStream") {
      withS3StreamStoreFixtures {
        case (bucket, table, hybridStore) =>
          val id = Random.nextString(5)
          val content = "A thousand thinking thanes thanking a therapod"
          val inputStream = new ByteArrayInputStream(content.getBytes)

          val future = hybridStore.updateRecord(id)(ifNotExisting =
            (inputStream, emptyMetadata))(ifExisting = (t, m) => (t, m))

          whenReady(future) { _ =>
            getContentFor(bucket, table, id) shouldBe content
          }
      }
    }

    it("retrieves an InputStream") {
      withS3StreamStoreFixtures {
        case (_, _, hybridStore) =>
          val id = Random.nextString(5)
          val content = "Five fishing flinging flint"
          val inputStream = new ByteArrayInputStream(content.getBytes)

          val putFuture =
            hybridStore.updateRecord(id)(ifNotExisting =
              (inputStream, emptyMetadata))(ifExisting = (t, m) => (t, m))

          val getFuture = putFuture.flatMap { _ =>
            hybridStore.getRecord(id)
          }

          whenReady(getFuture) { result =>
            result.map(stringify) shouldBe Some(content)
          }
      }
    }
  }
}
