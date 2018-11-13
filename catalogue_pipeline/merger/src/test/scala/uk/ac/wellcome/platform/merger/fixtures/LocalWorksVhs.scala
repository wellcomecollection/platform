package uk.ac.wellcome.platform.merger.fixtures

import org.scalatest.Assertion
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait LocalWorksVhs
    extends LocalVersionedHybridStore
    with Eventually
    with ScalaFutures {

  type TransformedBaseWorkVHS = VersionedHybridStore[
    TransformedBaseWork,
    EmptyMetadata,
    ObjectStore[TransformedBaseWork]]

  def withTransformedBaseWorkVHS[R](testWith: TestWith[TransformedBaseWorkVHS, R]): R =
    withLocalS3Bucket { storageBucket =>
      withLocalDynamoDbTable { table =>
        withTypeVHS[TransformedBaseWork, EmptyMetadata, R](storageBucket, table) { vhs =>
          testWith(vhs)
        }
      }
    }

  def givenStoredInVhs(
    vhs: VersionedHybridStore[TransformedBaseWork,
                              EmptyMetadata,
                              ObjectStore[TransformedBaseWork]],
    work: TransformedBaseWork): Assertion = {
    vhs.updateRecord(work.sourceIdentifier.toString)(
      ifNotExisting = (work, EmptyMetadata()))((_, _) =>
      throw new RuntimeException("Not possible, VHS is empty!"))

    eventually {
      whenReady(vhs.getRecord(id = work.sourceIdentifier.toString)) { result =>
        result.get shouldBe work
      }
    }
  }

  def givenStoredInVhs(
    vhs: VersionedHybridStore[TransformedBaseWork,
                              EmptyMetadata,
                              ObjectStore[TransformedBaseWork]],
    entries: List[TransformedBaseWork]): List[Assertion] =
    entries.map { work =>
      givenStoredInVhs(vhs = vhs, work = work)
    }
}
