package uk.ac.wellcome.platform.merger.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.WorkIdentifier
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.TransformedBaseWork

import scala.concurrent.ExecutionContext.Implicits.global

class RecorderPlaybackServiceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with LocalVersionedHybridStore
    with MergerTestUtils {

  it("fetches a single Work") {
    val work = createUnidentifiedWork

    withRecorderVHS { vhs =>
      storeInVHS(vhs, work)

      val service = new RecorderPlaybackService(vhs)

      whenReady(service.fetchAllWorks(getWorkIdentifiers(work))) { result =>
        result shouldBe List(Some(work))
      }
    }
  }

  it("throws an error if asked to fetch a missing entry") {
    val work = createUnidentifiedWork

    withRecorderVHS { vhs =>
      val service = new RecorderPlaybackService(vhs)

      whenReady(
        service
          .fetchAllWorks(getWorkIdentifiers(work))
          .failed) { result =>
        result shouldBe a[NoSuchElementException]
        result.getMessage shouldBe s"Work ${work.sourceIdentifier} is not in VHS!"
      }
    }
  }

  it("returns None if asked to fetch a Work with version 0") {
    val work = createUnidentifiedWorkWith(version = 0)

    withRecorderVHS { vhs =>
      val service = new RecorderPlaybackService(vhs)

      whenReady(service.fetchAllWorks(getWorkIdentifiers(work))) { result =>
        result shouldBe List(None)
      }
    }
  }

  it("returns None if the version in VHS has a higher version") {
    val work = createUnidentifiedWorkWith(version = 2)

    val workToStore = createUnidentifiedWorkWith(
      sourceIdentifier = work.sourceIdentifier,
      version = work.version + 1
    )

    withRecorderVHS { vhs =>
      storeInVHS(vhs, workToStore)

      val service = new RecorderPlaybackService(vhs)

      whenReady(service.fetchAllWorks(getWorkIdentifiers(work))) { result =>
        result shouldBe List(None)
      }
    }
  }

  it("gets a mixture of works as appropriate") {
    val worksToFetch = (1 to 3).map { _ =>
      createUnidentifiedWork
    }
    val outdatedWorks = (4 to 5).map { _ =>
      createUnidentifiedWork
    }
    val updatedWorks = outdatedWorks.map { work =>
      work.copy(version = work.version + 1)
    }
    val zeroWorks = (6 to 7).map { _ =>
      createUnidentifiedWorkWith(version = 0)
    }

    val allWorks = (worksToFetch ++ outdatedWorks ++ zeroWorks).toList

    withRecorderVHS { vhs =>
      storeInVHS(vhs, allWorks)

      val service = new RecorderPlaybackService(vhs)

      whenReady(service.fetchAllWorks(getWorkIdentifiers(allWorks: _*))) {
        result =>
          result shouldBe (allWorks.map { Some(_) } ++ (4 to 7).map { _ =>
            None
          }).toList
      }
    }
  }

  private def getWorkIdentifiers(
    works: TransformedBaseWork*): List[WorkIdentifier] =
    works.map { work =>
      WorkIdentifier(
        identifier = work.sourceIdentifier.toString,
        version = work.version
      )
    }.toList

  private def withRecorderVHS[R](
    testWith: TestWith[VersionedHybridStore[TransformedBaseWork,
                                            EmptyMetadata,
                                            ObjectStore[TransformedBaseWork]],
                       R]): R = {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withTypeVHS[TransformedBaseWork, EmptyMetadata, R](bucket, table) {
          vhs =>
            testWith(vhs)
        }
      }
    }
  }
}
