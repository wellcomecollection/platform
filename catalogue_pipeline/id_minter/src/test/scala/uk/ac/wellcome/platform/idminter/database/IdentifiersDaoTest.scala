package uk.ac.wellcome.platform.idminter.database

import java.sql.SQLIntegrityConstraintViolationException

import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.models.SourceIdentifier
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.IdentifiersMysqlLocal

import scala.util.{Failure, Success}

class IdentifiersDaoTest
    extends FunSpec
    with IdentifiersMysqlLocal
    with Matchers {

  val identifiersDao = new IdentifiersDao(DB.connect(), identifiersTable)

  describe("lookupID") {
    it(
      "should return a future of Some[Identifier] if it can find a matching MiroID in the DB") {
      val identifier = Identifier(
        CanonicalID = "A turtle turns to try to taste",
        MiroID = "A tangerine",
        ontologyType = "t-t-t-turtles"
      )
      assertInsertingIdentifierSucceeds(identifier)

      val sourceIdentifiers = List(
        SourceIdentifier(
          identifierScheme = IdentifierSchemes.miroImageNumber,
          value = identifier.MiroID
        ))

      val triedLookup = identifiersDao.lookupID(
        sourceIdentifiers = sourceIdentifiers,
        ontologyType = identifier.ontologyType
      )

      triedLookup shouldBe a[Success[Option[String]]]
      val maybeIdentifier = triedLookup.get
      maybeIdentifier shouldBe defined
      maybeIdentifier.get shouldBe identifier
    }
  }

  val miroSourceIdentifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.miroImageNumber,
    value = "V0023075"
  )

  val calmSourceIdentifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.calmAltRefNo,
    value = "MS.290"
  )

  describe(
    "lookupID should return a future of Some[Identifier] if it can find a matching ID") {
    it("Matching Miro ID, Calm ID and ontology type") {
      val identifier = Identifier(
        CanonicalID = "h2s6hz29",
        MiroID = miroSourceIdentifier.value,
        CalmAltRefNo = calmSourceIdentifier.value
      )
      assertInsertingIdentifierSucceeds(identifier)

      assertLookupIDFindsMatch(
        sourceIdentifiers = List(miroSourceIdentifier, calmSourceIdentifier),
        ontologyType = identifier.ontologyType,
        identifier = identifier
      )
      assertLookupIDFindsMatch(
        sourceIdentifiers = List(miroSourceIdentifier),
        ontologyType = identifier.ontologyType,
        identifier = identifier
      )
      assertLookupIDFindsMatch(
        sourceIdentifiers = List(calmSourceIdentifier),
        ontologyType = identifier.ontologyType,
        identifier = identifier
      )
    }

    it("Multiple Miro IDs with different ontology types") {
      val identifier = Identifier(
        CanonicalID = "t3qf9q24",
        MiroID = miroSourceIdentifier.value,
        ontologyType = "TestWork"
      )
      val identifierAlt = Identifier(
        CanonicalID = "zehhzpsr",
        MiroID = miroSourceIdentifier.value,
        ontologyType = "TestWorkAlt"
      )
      assertInsertingIdentifierSucceeds(identifier)
      assertInsertingIdentifierSucceeds(identifierAlt)

      for (id <- List(identifier, identifierAlt))
        assertLookupIDFindsMatch(
          sourceIdentifiers = List(miroSourceIdentifier),
          ontologyType = id.ontologyType,
          identifier = id
        )
    }

    it(
      "Only a Miro ID in the database, but searching for both Miro and Calm IDs") {
      val identifier = Identifier(
        CanonicalID = "hydmw9zy",
        MiroID = miroSourceIdentifier.value,
        CalmAltRefNo = calmSourceIdentifier.value
      )
      assertInsertingIdentifierSucceeds(identifier)

      assertLookupIDFindsMatch(
        sourceIdentifiers = List(miroSourceIdentifier, calmSourceIdentifier),
        ontologyType = identifier.ontologyType,
        identifier = identifier
      )
    }
  }

  describe(
    "lookupID should return a future of None if it can't find a matching ID") {
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

    it("right ontology type, wrong IDs") {
      val identifier = Identifier(
        CanonicalID = "eqg6v2ws",
        MiroID = "A misleading Miro ID",
        CalmAltRefNo = "A capricious Calm ID",
        ontologyType = "TestWork"
      )
      assertInsertingIdentifierSucceeds(identifier)

      assertLookupIDFindsNothing(
        sourceIdentifiers = List(calmSourceIdentifier),
        ontologyType = identifier.ontologyType
      )
      assertLookupIDFindsNothing(
        sourceIdentifiers = List(miroSourceIdentifier),
        ontologyType = identifier.ontologyType
      )
    }
  }

  private def assertLookupIDFindsMatch(
    sourceIdentifiers: List[SourceIdentifier],
    ontologyType: String = "TestWork",
    identifier: Identifier) = {
    val triedLookup = identifiersDao.lookupID(
      sourceIdentifiers = sourceIdentifiers,
      ontologyType = ontologyType
    )
    triedLookup shouldBe a[Success[Option[String]]]
    val maybeIdentifier = triedLookup.get
    maybeIdentifier shouldBe defined
    maybeIdentifier.get shouldBe identifier
  }

  private def assertLookupIDFindsNothing(
    sourceIdentifiers: List[SourceIdentifier],
    ontologyType: String = "TestWork") {
    val triedLookup = identifiersDao.lookupID(
      sourceIdentifiers = sourceIdentifiers,
      ontologyType = ontologyType
    )
    triedLookup shouldBe a[Success[Option[String]]]
    val maybeIdentifier = triedLookup.get
    maybeIdentifier shouldNot be(defined)
  }

  describe("saveIdentifier") {
    it("should insert the provided identifier into the database") {
      val identifier = Identifier(
        CanonicalID = "A provision of porpoises",
        MiroID = "A picture of pangolins",
        ontologyType = "Work"
      )
      identifiersDao.saveIdentifier(identifier)
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

    it(
      "should allow saving two records with the same MiroID but different ontologyType") {
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

    it(
      "should allow saving two records with different MiroID but the same ontologyType") {
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

    it(
      "should reject inserting two records with the same MiroID and ontologyType") {
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

  /* Helper method.  Given two records, try to insert them both, and check
   * that integrity checks in the database reject the second record.
   */
  private def assertInsertingDuplicateFails(
    identifier: Identifier,
    duplicateIdentifier: Identifier) = {
    assertInsertingIdentifierSucceeds(identifier)

    val triedSave = identifiersDao.saveIdentifier(duplicateIdentifier)
    triedSave shouldBe a[Failure[SQLIntegrityConstraintViolationException]]
  }

  /* Helper method.  Insert a record and check that it succeeds. */
  private def assertInsertingIdentifierSucceeds(identifier: Identifier) = {
    val triedSave = identifiersDao.saveIdentifier(identifier)
    triedSave shouldBe a[Success[String]]
    triedSave shouldBe Success(1)
  }
}
