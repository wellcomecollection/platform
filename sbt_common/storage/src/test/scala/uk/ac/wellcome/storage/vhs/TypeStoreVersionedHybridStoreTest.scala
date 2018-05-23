package uk.ac.wellcome.storage.vhs

import java.io.{ByteArrayInputStream, InputStream}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.storage.s3.S3TypeStore
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.type_classes.{
  StorageKey,
  StorageStrategy,
  StorageStream
}
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

case class ExampleRecord(
  override val id: String,
  content: String
) extends Id

class TypeStoreVersionedHybridStoreTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with LocalVersionedHybridStore {

  import uk.ac.wellcome.storage.dynamo._

  implicit val store: StorageStrategy[ExampleRecord] =
    new StorageStrategy[ExampleRecord] {
      def store(t: ExampleRecord): StorageStream = {
        val key = StorageKey("key")
        val input = new ByteArrayInputStream(toJson(t).get.getBytes)

        StorageStream(input, key)
      }

      def retrieve(input: InputStream) =
        fromJson[ExampleRecord](Source.fromInputStream(input).mkString)

    }

  def withS3TypeStoreFixtures[R](
    testWith: TestWith[
      (Bucket,
       Table,
       VersionedHybridStore[ExampleRecord, S3TypeStore[ExampleRecord]]),
      R]
  ): R =
    withLocalS3Bucket[R] { bucket =>
      withLocalDynamoDbTable[R] { table =>
        withTypeVHS[ExampleRecord, R](bucket, table) { vhs =>
          testWith((bucket, table, vhs))
        }
      }
    }

  describe("with S3TypeStore") {
    it("stores the specified Type") {
      withS3TypeStoreFixtures {
        case (bucket, table, hybridStore) =>
          val record = ExampleRecord(
            id = Random.nextString(5),
            content = "One ocelot in orange"
          )

          val future =
            hybridStore.updateRecord(record.id)(record)(identity)()

          whenReady(future) { _ =>
            getJsonFor(bucket, table, record) shouldBe toJson(record).get
          }
      }
    }

    it("retrieves the specified type") {
      withS3TypeStoreFixtures {
        case (bucket, table, hybridStore) =>
          val id = Random.nextString(5)
          val record = ExampleRecord(
            id = Random.nextString(5),
            content = "Hairy hyenas howling hatefully"
          )
          val putFuture =
            hybridStore.updateRecord(id)(record)(identity)()

          val getFuture = putFuture.flatMap { _ =>
            hybridStore.getRecord(id)
          }

          whenReady(getFuture) { result =>
            result shouldBe Some(record)
          }
      }
    }
  }
}
