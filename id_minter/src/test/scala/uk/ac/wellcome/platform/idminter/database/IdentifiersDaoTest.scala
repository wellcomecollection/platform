package uk.ac.wellcome.platform.idminter.database

import java.sql.SQLIntegrityConstraintViolationException

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.IdentifiersMysqlLocal

class IdentifiersDaoTest
    extends FunSpec
    with IdentifiersMysqlLocal
    with ScalaFutures
    with Matchers {

  val identifiersDao = new IdentifiersDao(DB.connect(), identifiersTable)

  describe("lookupCanonicalID") {
    it("should return a future of Some[Identifier] if it can find a Canonical ID in the DB") {
      val identifier = Identifier(
        CanonicalID = "A canonical cat",
        MiroID = "A curious cheetah"
      )
      insertIdentifier(identifier)

      whenReady(identifiersDao.lookupCanonicalID(identifier.CanonicalID)) { maybeIdentifier =>
        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe identifier
      }
    }

    it("should return a future of None if looking up a non-existent Canonical ID") {
      whenReady(identifiersDao.lookupCanonicalID("A vanishing vulture")) { maybeIdentifier =>
        maybeIdentifier shouldNot be(defined)
      }
    }
  }

  describe("findSourceIdInDb") {
    it("should return a future of Some[Identifier] if it can find a MiroID in the DB") {
      val identifier = Identifier(
        CanonicalID = "A sand snail",
        MiroID = "A soft shell"
      )
      insertIdentifier(identifier)

      whenReady(identifiersDao.findSourceIdInDb(identifier.MiroID)) { maybeIdentifier =>
        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe identifier
      }
    }

    it("should return a future of None if looking up a non-existent Miro ID") {
      whenReady(identifiersDao.findSourceIdInDb("A missing mouse")) { maybeIdentifier =>
        maybeIdentifier shouldNot be(defined)
      }
    }
  }

  describe("saveIdentifier") {

    it("should insert the provided identifier into the database") {
      val identifier = Identifier(CanonicalID = "5678", MiroID = "1234")
      val future = identifiersDao.saveIdentifier(identifier)

      whenReady(future) { _ =>
        val maybeIdentifier = withSQL {
          select
            .from(identifiersTable as identifiersTable.i)
            .where
            .eq(identifiersTable.i.MiroID, identifier.MiroID)
            .and
            .eq(identifiersTable.i.CanonicalID, identifier.CanonicalID)
        }.map(Identifier(identifiersTable.i)).single.apply()

        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe identifier
      }
    }

    it(
      "should fail inserting a record if there is already a record for the same miroId") {
      val identifier = new Identifier(CanonicalID = "5678", MiroID = "1234")
      insertIdentifier(identifier)

      val saveCanonicalId =
        identifiersDao.saveIdentifier(identifier.copy(CanonicalID = "0987"))
      whenReady(saveCanonicalId.failed) { exception =>
        exception shouldBe a[SQLIntegrityConstraintViolationException]
      }
    }
  }

  private def insertIdentifier(identifier: Identifier) =
    whenReady(identifiersDao.saveIdentifier(identifier)) { result =>
      result shouldBe 1
    }
}
