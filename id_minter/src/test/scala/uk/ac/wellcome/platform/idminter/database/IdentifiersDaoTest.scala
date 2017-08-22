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

  describe("lookupMiroID") {
    it("should return a future of Some[Identifier] if it can find a MiroID in the DB") {
      val identifier = Identifier(
        CanonicalID = "A sand snail",
        MiroID = "A soft shell",
        ontologyType = "Work"
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
        MiroID = "A picture of pangolins",
        ontologyType = "Work"
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
        MiroID = "A farm full of fruit",
        ontologyType = "Work"
      )
      val duplicateIdentifier = new Identifier(
        CanonicalID = identifier.CanonicalID,
        MiroID = "Fuel for a factory",
        ontologyType = "Work"
      )

      assertInsertingDuplicateFails(identifier, duplicateIdentifier)
    }

    it("should fail to insert a record with a duplicate MiroID") {
      val identifier = new Identifier(
        CanonicalID = "A picking of parsley",
        MiroID = "A packet of peppermints",
        ontologyType = "Work"
      )
      val duplicateIdentifier = new Identifier(
        CanonicalID = "A portion of potatoes",
        MiroID = identifier.MiroID,
        ontologyType = "Work"
      )

      assertInsertingDuplicateFails(identifier, duplicateIdentifier)
    }
  }

  /** Helper method.  Given two records, try to insert them both, and check
    * that integrity checks in the database reject the second record.
    */
  private def assertInsertingDuplicateFails(identifier: Identifier,
                                            duplicateIdentifier: Identifier) = {
    insertIdentifier(identifier)

    val duplicateFuture = identifiersDao.saveIdentifier(duplicateIdentifier)
    whenReady(duplicateFuture.failed) { exception =>
      exception shouldBe a[SQLIntegrityConstraintViolationException]
    }
  }

  /** Helper method.  Insert a record and check that it succeeds. */
  private def insertIdentifier(identifier: Identifier) =
    whenReady(identifiersDao.saveIdentifier(identifier)) { result =>
      result shouldBe 1
    }
}
