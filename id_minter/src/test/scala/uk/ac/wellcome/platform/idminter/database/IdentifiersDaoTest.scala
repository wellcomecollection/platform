package uk.ac.wellcome.platform.idminter.database

import java.sql.SQLIntegrityConstraintViolationException

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.models.SourceIdentifier
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.IdentifiersMysqlLocal

class IdentifiersDaoTest
    extends FunSpec
    with IdentifiersMysqlLocal
    with ScalaFutures
    with Matchers {

  val identifiersDao = new IdentifiersDao(DB.connect(), identifiersTable)

  describe("lookupID") {
    it("should return a future of Some[Identifier] if it can find a matching MiroID in the DB") {
      val identifier = Identifier(
        CanonicalID = "A turtle turns to try to taste",
        MiroID = "A tangerine",
        ontologyType = "t-t-t-turtles"
      )
      assertInsertingIdentifierSucceeds(identifier)

      val sourceIdentifiers = List(SourceIdentifier(
        identifierScheme = "miro-image-number",
        value = identifier.MiroID
      ))

      val lookupFuture = identifiersDao.lookupID(
        sourceIdentifiers = sourceIdentifiers,
        ontologyType = identifier.ontologyType
      )
      whenReady(lookupFuture) { maybeIdentifier =>
        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe identifier
      }
    }
  }

  val miroSourceIdentifier = SourceIdentifier(
    identifierScheme = "miro-image-number",
    value = "V0023075"
  )

  val calmSourceIdentifier = SourceIdentifier(
    identifierScheme = "calm-altrefno",
    value = "MS.290"
  )

  describe("lookupID should return a future of None if it can't find a matching ID") {
    it("empty database, looking for a Miro ID") {
      assertLookupIDFindsNothing(
        sourceIdentifiers = List(miroSourceIdentifier)
      )
    }

    it("empty database, looking for a Calm AltRefNo") {
      assertLookupIDFindsNothing(
        sourceIdentifiers = List(calmSourceIdentifier)
      )
    }

    it("empty database, looking for a Miro ID and a Calm AltRefNo") {
      assertLookupIDFindsNothing(
        sourceIdentifiers = List(miroSourceIdentifier, calmSourceIdentifier)
      )
    }

    it("matching Miro ID, wrong ontology type") {
      val identifier = Identifier(
        CanonicalID = "qk9yeajr",
        MiroID = miroSourceIdentifier.value,
        ontologyType = "TestItem"
      )
      assertInsertingIdentifierSucceeds(identifier)

      assertLookupIDFindsNothing(
        sourceIdentifiers = List(miroSourceIdentifier),
        ontologyType = "TestWork"
      )
      assertLookupIDFindsNothing(
        sourceIdentifiers = List(miroSourceIdentifier, calmSourceIdentifier),
        ontologyType = "TestWork"
      )
    }

    it("matching Calm AltRefNo, wrong ontology type") {
      val identifier = Identifier(
        CanonicalID = "pptk9sz6",
        CalmAltRefNo = calmSourceIdentifier.value,
        ontologyType = "TestItem"
      )
      assertInsertingIdentifierSucceeds(identifier)

      assertLookupIDFindsNothing(
        sourceIdentifiers = List(calmSourceIdentifier),
        ontologyType = "TestWork"
      )
      assertLookupIDFindsNothing(
        sourceIdentifiers = List(calmSourceIdentifier, miroSourceIdentifier),
        ontologyType = "TestWork"
      )
    }

    it("matching Calm AltRefNo and Miro ID, wrong ontology type") {
      val identifier = Identifier(
        CanonicalID = "w9wr583y",
        CalmAltRefNo = calmSourceIdentifier.value,
        MiroID = miroSourceIdentifier.value,
        ontologyType = "TestItem"
      )
      assertInsertingIdentifierSucceeds(identifier)

      assertLookupIDFindsNothing(
        sourceIdentifiers = List(calmSourceIdentifier),
        ontologyType = "TestWork"
      )
      assertLookupIDFindsNothing(
        sourceIdentifiers = List(miroSourceIdentifier),
        ontologyType = "TestWork"
      )
      assertLookupIDFindsNothing(
        sourceIdentifiers = List(calmSourceIdentifier, miroSourceIdentifier),
        ontologyType = "TestWork"
      )
    }

    it("matching Miro ID, wrong Calm AltRefNo") {
      val identifier = Identifier(
        CanonicalID = "qs5apdq8",
        MiroID = miroSourceIdentifier.value,
        CalmAltRefNo = "Not a real Calm AltRefNo",
        ontologyType = "TestWork"
      )
      assertInsertingIdentifierSucceeds(identifier)

      assertLookupIDFindsNothing(
        sourceIdentifiers = List(miroSourceIdentifier, calmSourceIdentifier),
        ontologyType = identifier.ontologyType
      )
    }

    it("matching Calm AltRefNo, wrong Miro ID") {
      val identifier = Identifier(
        CanonicalID = "qs5apdq8",
        MiroID = "Not a real MiroID",
        CalmAltRefNo = calmSourceIdentifier.value,
        ontologyType = "TestWork"
      )
      assertInsertingIdentifierSucceeds(identifier)

      assertLookupIDFindsNothing(
        sourceIdentifiers = List(miroSourceIdentifier, calmSourceIdentifier),
        ontologyType = identifier.ontologyType
      )
    }
  }

  private def assertLookupIDFindsNothing(sourceIdentifiers: List[SourceIdentifier],
                                         ontologyType: String = "TestWork") {
    val lookupFuture = identifiersDao.lookupID(
      sourceIdentifiers = sourceIdentifiers,
      ontologyType = ontologyType
    )
    whenReady(lookupFuture) { maybeIdentifier =>
      maybeIdentifier shouldNot be(defined)
    }
  }



  describe("lookupMiroID") {
    it("should return a future of Some[Identifier] if it can find a MiroID in the DB") {
      val identifier = Identifier(
        CanonicalID = "A sand snail",
        MiroID = "A soft shell"
      )
      assertInsertingIdentifierSucceeds(identifier)

      whenReady(identifiersDao.lookupMiroID(identifier.MiroID)) { maybeIdentifier =>
        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe identifier
      }
    }

    it("should return a future of None if looking up a non-existent Miro ID") {
      assertLookupMiroIDFindsNothing(
        miroID = "A missing mouse"
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
  private def assertLookupMiroIDFindsNothing(miroID: String, ontologyType: String = "Work") = {
    val lookupFuture = identifiersDao.lookupMiroID(
      miroID = miroID,
      ontologyType = ontologyType
    )
    whenReady(lookupFuture) { maybeIdentifier =>
      maybeIdentifier shouldNot be(defined)
    }
  }
}
