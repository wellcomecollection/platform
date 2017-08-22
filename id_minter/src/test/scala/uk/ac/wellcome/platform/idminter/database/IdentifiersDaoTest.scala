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

  describe("lookupMiroID") {
    it("should return a future of Some[Identifier] if it can find a MiroID in the DB") {
      val identifier = Identifier(
        CanonicalID = "A sand snail",
        MiroID = "A soft shell"
      )
      insertIdentifier(identifier)

      whenReady(identifiersDao.lookupMiroID(identifier.MiroID)) { maybeIdentifier =>
        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe identifier
      }
    }

    it("should return a future of None if looking up a non-existent Miro ID") {
      whenReady(identifiersDao.lookupMiroID("A missing mouse")) { maybeIdentifier =>
        maybeIdentifier shouldNot be(defined)
      }
    }
  }

  describe("saveIdentifier") {
    it("should insert the provided identifier into the database") {
      val identifier = Identifier(
        CanonicalID = "A provision of porpoises",
        MiroID = "A picture of pangolins"
      )
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

    it("should fail to insert a record with a duplicate CanonicalID") {
      val identifier = new Identifier(
        CanonicalID = "A failed field of flowers",
        MiroID = "A farm full of fruit"
      )
      val duplicateIdentifier = new Identifier(
        CanonicalID = identifier.CanonicalID,
        MiroID = "Fuel for a factory"
      )

      assertInsertingDuplicateFails(identifier, duplicateIdentifier)
    }

    it("should fail to insert a record with a duplicate MiroID") {
      val identifier = new Identifier(
        CanonicalID = "A picking of parsley",
        MiroID = "A packet of peppermints"
      )
      val duplicateIdentifier = new Identifier(
        CanonicalID = "A portion of potatoes",
        MiroID = identifier.MiroID
      )

      assertInsertingDuplicateFails(identifier, duplicateIdentifier)
    }
  }

  private def assertInsertingDuplicateFails(identifier: Identifier,
                                            duplicateIdentifier: Identifier) = {
    insertIdentifier(identifier)

    val duplicateFuture = identifiersDao.saveIdentifier(duplicateIdentifier)
    whenReady(duplicateFuture.failed) { exception =>
      exception shouldBe a[SQLIntegrityConstraintViolationException]
    }
  }

  private def insertIdentifier(identifier: Identifier) =
    whenReady(identifiersDao.saveIdentifier(identifier)) { result =>
      result shouldBe 1
    }
}
