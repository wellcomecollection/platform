package uk.ac.wellcome.platform.idminter.database

import java.sql.SQLIntegrityConstraintViolationException

import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.platform.idminter.models.Identifier
import uk.ac.wellcome.platform.idminter.utils.IdentifiersMysqlLocal

import scala.util.{Failure, Success}

class IdentifiersDaoTest
    extends FunSpec
    with IdentifiersMysqlLocal
    with Matchers {

  val identifiersDao = new IdentifiersDao(DB.connect(), identifiersTable)

  describe("lookupID") {
    it(
      "gets an Identifier if it can find a matching SourceSystem and SourceId") {
      val identifier = Identifier(
        CanonicalId = "A turtle turns to try to taste",
        SourceId = "A tangerine",
        SourceSystem = IdentifierSchemes.miroImageNumber.toString,
        OntologyType = "t-t-t-turtles"
      )
      assertInsertingIdentifierSucceeds(identifier)

      val sourceIdentifier = SourceIdentifier(
        identifierScheme = IdentifierSchemes.miroImageNumber,
        value = identifier.SourceId
      )

      val triedLookup = identifiersDao.lookupId(
        sourceIdentifier = sourceIdentifier,
        ontologyType = identifier.OntologyType
      )

      triedLookup shouldBe Success(Some(identifier))
    }

    it("gets no identifier if there is no matching SourceSystem and SourceId") {
      val identifier = Identifier(
        CanonicalId = "A turtle turns to try to taste",
        SourceId = "A tangerine",
        SourceSystem = IdentifierSchemes.miroImageNumber.toString,
        OntologyType = "t-t-t-turtles"
      )
      assertInsertingIdentifierSucceeds(identifier)

      val sourceIdentifier = SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraSystemNumber,
        value = "not_an_existing_value"
      )

      val triedLookup = identifiersDao.lookupId(
        sourceIdentifier = sourceIdentifier,
        ontologyType = identifier.OntologyType
      )

      triedLookup shouldBe Success(None)
    }
  }

  private def assertLookupIDFindsMatch(sourceIdentifier: SourceIdentifier,
                                       ontologyType: String = "TestWork") = {

    val triedLookup = identifiersDao.lookupId(
      sourceIdentifier = sourceIdentifier,
      ontologyType = ontologyType
    )

    val identifier = triedLookup.get.get
    identifier shouldBe identifier
  }

  private def assertLookupIDFindsNothing(sourceIdentifier: SourceIdentifier,
                                         ontologyType: String = "TestWork") {

    val triedLookup = identifiersDao.lookupId(
      sourceIdentifier = sourceIdentifier,
      ontologyType = ontologyType
    )

    val identifier = triedLookup.get
    identifier shouldBe None
  }

  describe("saveIdentifier") {
    it("should insert the provided identifier into the database") {
      val identifier = Identifier(
        CanonicalId = "A provision of porpoises",
        OntologyType = "Work",
        SourceSystem = IdentifierSchemes.miroImageNumber.toString,
        SourceId = "A picture of pangolins"
      )
      identifiersDao.saveIdentifier(identifier)
      val maybeIdentifier = withSQL {
        select
          .from(identifiersTable as identifiersTable.i)
          .where
          .eq(
            identifiersTable.i.SourceSystem,
            IdentifierSchemes.miroImageNumber.toString)
          .and
          .eq(identifiersTable.i.CanonicalId, identifier.CanonicalId)
      }.map(Identifier(identifiersTable.i)).single.apply()

      maybeIdentifier shouldBe defined
      maybeIdentifier.get shouldBe identifier
    }

    it("should fail to insert a record with a duplicate CanonicalId") {
      val identifier = new Identifier(
        CanonicalId = "A failed field of flowers",
        SourceId = "A farm full of fruit",
        SourceSystem = "France",
        OntologyType = "Fruits"
      )
      val duplicateIdentifier = new Identifier(
        CanonicalId = identifier.CanonicalId,
        SourceId = "Fuel for a factory",
        SourceSystem = "Space",
        OntologyType = "Fuels"
      )

      assertInsertingDuplicateFails(identifier, duplicateIdentifier)
    }

    it(
      "should save records with the same SourceSystem and SourceId but different OntologyType") {
      val identifier = new Identifier(
        CanonicalId = "A mountain of muesli",
        SourceSystem = "A maize made of maze",
        SourceId = "A maize made of maze",
        OntologyType = "Cereals"
      )

      val secondIdentifier = new Identifier(
        CanonicalId = "A mere mango",
        SourceSystem = identifier.SourceSystem,
        SourceId = "A maize made of maze",
        OntologyType = "Fruits"
      )
      assertInsertingIdentifierSucceeds(identifier)
      assertInsertingIdentifierSucceeds(secondIdentifier)
    }

    it(
      "should save records with different SourceId but the same OntologyType and SourceSystem") {
      val identifier = new Identifier(
        CanonicalId = "Overflowing with okra",
        SourceId = "Olive oil in an orchard",
        SourceSystem = "A hedge maze in Loughborough",
        OntologyType = "Crops"
      )
      val secondIdentifier = new Identifier(
        CanonicalId = "An order of onions",
        SourceId = "Only orange orbs",
        SourceSystem = "A hedge maze in Loughborough",
        OntologyType = identifier.OntologyType
      )
      assertInsertingIdentifierSucceeds(identifier)
      assertInsertingIdentifierSucceeds(secondIdentifier)
    }

    it(
      "should not insert records with the same SourceId, SourceSystem and OntologyType") {
      val identifier = new Identifier(
        CanonicalId = "A surplus of strawberries",
        SourceId = "Sunflower seeds in a sack",
        SourceSystem = "The microscopic world of eyelash lice",
        OntologyType = "Cropssss"
      )
      val duplicateIdentifier = new Identifier(
        CanonicalId = "Sweeteners and sugar cane",
        SourceId = identifier.SourceId,
        SourceSystem = identifier.SourceSystem,
        OntologyType = identifier.OntologyType
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
    triedSave shouldBe a[Failure[_]]
    triedSave.failed.get shouldBe a[SQLIntegrityConstraintViolationException]
  }

  /* Helper method.  Insert a record and check that it succeeds. */
  private def assertInsertingIdentifierSucceeds(identifier: Identifier) = {
    val triedSave = identifiersDao.saveIdentifier(identifier)
    triedSave shouldBe Success(1)
  }
}
