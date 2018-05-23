package uk.ac.wellcome.storage.vhs

import java.io.{ByteArrayInputStream, InputStream}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.storage.s3.S3StreamStore
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.type_classes.{
  StorageStrategy,
  StorageStrategyGenerator
}
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

class StreamStoreVersionedHybridStoreTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with LocalVersionedHybridStore {

  import uk.ac.wellcome.storage.dynamo._

  implicit val store: StorageStrategy[InputStream] =
    StorageStrategyGenerator.streamStore

  private def stringify(is: InputStream) =
    scala.io.Source.fromInputStream(is).mkString

  def withS3StreamStoreFixtures[R](
    testWith: TestWith[(Bucket,
                        Table,
                        VersionedHybridStore[InputStream, S3StreamStore]),
                       R]
  ): R =
    withLocalS3Bucket[R] { bucket =>
      withLocalDynamoDbTable[R] { table =>
        withStreamVHS[R](bucket, table) { vhs =>
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

          val future = hybridStore.updateRecord(id)(inputStream)(identity)()

          whenReady(future) { _ =>
            getContentFor(bucket, table, id) shouldBe content
          }
      }
    }

    it("retrieves an InputStream") {
      withS3StreamStoreFixtures {
        case (bucket, table, hybridStore) =>
          val id = Random.nextString(5)
          val content = "Five fishing flinging flint"
          val inputStream = new ByteArrayInputStream(content.getBytes)

          val putFuture =
            hybridStore.updateRecord(id)(inputStream)(identity)()

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
