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
        ontologyType = "TestWork"
      )
      assertInsertingIdentifierSucceeds(identifier)

      val lookupFuture = identifiersDao.lookupMiroID(
        miroID = identifier.MiroID,
        ontologyType = identifier.ontologyType
      )

      whenReady(lookupFuture) { maybeIdentifier =>
        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe identifier
      }
    }

    it("should return a future of None if looking up a non-existent Miro ID") {
      assertLookupMiroIDFindsNothing(
        miroID = "A missing mouse",
        ontologyType = "TestWork"
      )
    }

    it("should return a future of None if looking up a Miro ID with the wrong ontologyType") {
      val identifier = Identifier(
        CanonicalID = "A sprinkling of sage",
        MiroID = "Seasoning with saffron",
        ontologyType = "Herbs"
      )
      assertInsertingIdentifierSucceeds(identifier)

      assertLookupMiroIDFindsNothing(
        miroID = identifier.MiroID,
        ontologyType = "Vegetables"
      )
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
        ontologyType = "Fruits"
      )
      val duplicateIdentifier = new Identifier(
        CanonicalID = identifier.CanonicalID,
        MiroID = "Fuel for a factory",
        ontologyType = "Fuels"
      )

      assertInsertingDuplicateFails(identifier, duplicateIdentifier)
    }

    it("should allow saving two records with the same MiroID but different ontologyType") {
      val identifier = new Identifier(
        CanonicalID = "A mountain of muesli",
        MiroID = "A maize made of maze",
        ontologyType = "Cereals"
      )
      val secondIdentifier = new Identifier(
        CanonicalID = "A mere mango",
        MiroID = identifier.MiroID,
        ontologyType = "Fruits"
      )
      assertInsertingIdentifierSucceeds(identifier)
      assertInsertingIdentifierSucceeds(secondIdentifier)
    }

    it("should allow saving two records with different MiroID but the same ontologyType") {
      val identifier = new Identifier(
        CanonicalID = "Overflowing with okra",
        MiroID = "Olive oil in an orchard",
        ontologyType = "Crops"
      )
      val secondIdentifier = new Identifier(
        CanonicalID = "An order of onions",
        MiroID = "Only orange orbs",
        ontologyType = identifier.ontologyType
      )
      assertInsertingIdentifierSucceeds(identifier)
      assertInsertingIdentifierSucceeds(secondIdentifier)
    }

    it("should reject inserting two records with the same MiroID and ontologyType") {
      val identifier = new Identifier(
        CanonicalID = "A surplus of strawberries",
        MiroID = "Sunflower seeds in a sack",
        ontologyType = "Cropssss"
      )
      val duplicateIdentifier = new Identifier(
        CanonicalID = "Sweeteners and sugar cane",
        MiroID = identifier.MiroID,
        ontologyType = identifier.ontologyType
      )

      assertInsertingDuplicateFails(identifier, duplicateIdentifier)
    }
  }

  /** Helper method.  Given two records, try to insert them both, and check
    * that integrity checks in the database reject the second record.
    */
  private def assertInsertingDuplicateFails(identifier: Identifier,
                                            duplicateIdentifier: Identifier) = {
    assertInsertingIdentifierSucceeds(identifier)

    val duplicateFuture = identifiersDao.saveIdentifier(duplicateIdentifier)
    whenReady(duplicateFuture.failed) { exception =>
      exception shouldBe a[SQLIntegrityConstraintViolationException]
    }
  }

  /** Helper method.  Insert a record and check that it succeeds. */
  private def assertInsertingIdentifierSucceeds(identifier: Identifier) =
    whenReady(identifiersDao.saveIdentifier(identifier)) { result =>
      result shouldBe 1
    }

  /** Helper method.  Do a Miro ID lookup and check that it fails. */
  private def assertLookupMiroIDFindsNothing(miroID: String, ontologyType: String) = {
    val lookupFuture = identifiersDao.lookupMiroID(
      miroID = miroID,
      ontologyType = ontologyType
    )
    whenReady(lookupFuture) { maybeIdentifier =>
      maybeIdentifier shouldNot be(defined)
    }
  }
}
