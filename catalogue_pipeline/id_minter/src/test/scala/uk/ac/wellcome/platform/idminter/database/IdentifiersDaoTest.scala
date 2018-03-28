package uk.ac.wellcome.platform.idminter.database

import java.sql.SQLIntegrityConstraintViolationException

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.platform.idminter.fixtures.DatabaseConfig
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.util.{Failure, Success}

case class IdentifiersDaoFixtures(
  identifiersDao: IdentifiersDao,
  identifiersTable: IdentifiersTable,
  dbConfig: DatabaseConfig
)

class IdentifiersDaoTest
    extends FunSpec
    with fixtures.IdentifiersDatabase
    with Matchers {

  def withIdentifiersDao[R](testWith: TestWith[IdentifiersDaoFixtures, R]) =
    withIdentifiersDatabase { dbConfig =>
      val identifiersTable: IdentifiersTable =
        new IdentifiersTable(dbConfig.databaseName, dbConfig.tableName)

      new TableProvisioner(host, port, username, password)
        .provision(dbConfig.databaseName, dbConfig.tableName)

      val identifiersDao = new IdentifiersDao(DB.connect(), identifiersTable)

      eventuallyTableExists(dbConfig)

      testWith(
        IdentifiersDaoFixtures(identifiersDao, identifiersTable, dbConfig))
    }

  describe("lookupID") {
    it("gets an Identifier if it finds a matching SourceSystem and SourceId") {
      withIdentifiersDao { fixtures =>
        val identifier = Identifier(
          CanonicalId = "A turtle turns to try to taste",
          SourceId = "A tangerine",
          SourceSystem = IdentifierSchemes.miroImageNumber.toString,
          OntologyType = "t-t-t-turtles"
        )
        fixtures.identifiersDao.saveIdentifier(identifier) shouldBe (Success(
          1))

        val sourceIdentifier = SourceIdentifier(
          identifierScheme = IdentifierSchemes.miroImageNumber,
          identifier.OntologyType,
          value = identifier.SourceId
        )

        val triedLookup = fixtures.identifiersDao.lookupId(
          sourceIdentifier = sourceIdentifier
        )

        triedLookup shouldBe Success(Some(identifier))
      }
    }

    it(
      "does not get an identifier if there is no matching SourceSystem and SourceId") {
      withIdentifiersDao { fixtures =>
        val identifier = Identifier(
          CanonicalId = "A turtle turns to try to taste",
          SourceId = "A tangerine",
          SourceSystem = IdentifierSchemes.miroImageNumber.toString,
          OntologyType = "t-t-t-turtles"
        )

        fixtures.identifiersDao.saveIdentifier(identifier) shouldBe (Success(
          1))

        val sourceIdentifier = SourceIdentifier(
          identifierScheme = IdentifierSchemes.sierraSystemNumber,
          identifier.OntologyType,
          value = "not_an_existing_value"
        )

        val triedLookup = fixtures.identifiersDao.lookupId(
          sourceIdentifier = sourceIdentifier
        )

        triedLookup shouldBe Success(None)
      }
    }
  }

  private def assertLookupIDFindsMatch(identifiersDao: IdentifiersDao,
                                       sourceIdentifier: SourceIdentifier,
                                       ontologyType: String = "TestWork") = {

    val triedLookup = identifiersDao.lookupId(
      sourceIdentifier = sourceIdentifier
    )

    val identifier = triedLookup.get.get
    identifier shouldBe identifier
  }

  private def assertLookupIDFindsNothing(identifiersDao: IdentifiersDao,
                                         sourceIdentifier: SourceIdentifier,
                                         ontologyType: String = "TestWork") {

    val triedLookup = identifiersDao.lookupId(
      sourceIdentifier = sourceIdentifier
    )

    val identifier = triedLookup.get
    identifier shouldBe None
  }

  describe("saveIdentifier") {
    it("inserts the provided identifier into the database") {
      withIdentifiersDao { fixtures =>
        implicit val session = fixtures.dbConfig.session

        val identifier = Identifier(
          CanonicalId = "A provision of porpoises",
          OntologyType = "Work",
          SourceSystem = IdentifierSchemes.miroImageNumber.toString,
          SourceId = "A picture of pangolins"
        )
        fixtures.identifiersDao.saveIdentifier(identifier)
        val maybeIdentifier = withSQL {
          select
            .from(fixtures.identifiersTable as fixtures.identifiersTable.i)
            .where
            .eq(
              fixtures.identifiersTable.i.SourceSystem,
              IdentifierSchemes.miroImageNumber.toString)
            .and
            .eq(
              fixtures.identifiersTable.i.CanonicalId,
              identifier.CanonicalId)
        }.map(Identifier(fixtures.identifiersTable.i)).single.apply()

        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe identifier
      }
    }

    it("fails to insert a record with a duplicate CanonicalId") {
      withIdentifiersDao { fixtures =>
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

        fixtures.identifiersDao.saveIdentifier(identifier) shouldBe (Success(
          1))

        val triedSave =
          fixtures.identifiersDao.saveIdentifier(duplicateIdentifier)

        triedSave shouldBe a[Failure[_]]
        triedSave.failed.get shouldBe a[
          SQLIntegrityConstraintViolationException]
      }
    }

    it(
      "saves records with the same SourceSystem and SourceId but different OntologyType") {
      withIdentifiersDao { fixtures =>
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

        fixtures.identifiersDao.saveIdentifier(identifier) shouldBe (Success(
          1))
        fixtures.identifiersDao.saveIdentifier(secondIdentifier) shouldBe (Success(
          1))
      }
    }

    it(
      "saves records with different SourceId but the same OntologyType and SourceSystem") {
      withIdentifiersDao { fixtures =>
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

        fixtures.identifiersDao.saveIdentifier(identifier) shouldBe (Success(
          1))
        fixtures.identifiersDao.saveIdentifier(secondIdentifier) shouldBe (Success(
          1))
      }
    }

    it(
      "does not insert records with the same SourceId, SourceSystem and OntologyType") {
      withIdentifiersDao { fixtures =>
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

        fixtures.identifiersDao.saveIdentifier(identifier) shouldBe (Success(
          1))

        val triedSave =
          fixtures.identifiersDao.saveIdentifier(duplicateIdentifier)

        triedSave shouldBe a[Failure[_]]
        triedSave.failed.get shouldBe a[
          SQLIntegrityConstraintViolationException]
      }
    }
  }
}
