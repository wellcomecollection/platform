package uk.ac.wellcome.storage.vhs

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

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

  def withS3TypeStoreFixtures[R](
    testWith: TestWith[(Bucket,
                        Table,
                        VersionedHybridStore[ExampleRecord,
                                             EmptyMetadata,
                                             ObjectStore[ExampleRecord]]),
                       R]
  ): R =
    withLocalS3Bucket[R] { bucket =>
      withLocalDynamoDbTable[R] { table =>
        withTypeVHS[ExampleRecord, EmptyMetadata, R](bucket, table) { vhs =>
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

          val future = hybridStore.updateRecord(record.id)(ifNotExisting =
            (record, EmptyMetadata()))(ifExisting = (t, m) => (t, m))

          whenReady(future) { _ =>
            getJsonFor(bucket, table, record) shouldBe toJson(record).get
          }
      }
    }

    it("retrieves the specified type") {
      withS3TypeStoreFixtures {
        case (_, _, hybridStore) =>
          val id = Random.nextString(5)
          val record = ExampleRecord(
            id = Random.nextString(5),
            content = "Hairy hyenas howling hatefully"
          )
          val putFuture =
            hybridStore.updateRecord(id)(ifNotExisting =
              (record, EmptyMetadata()))(ifExisting = (t, m) => (t, m))

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
