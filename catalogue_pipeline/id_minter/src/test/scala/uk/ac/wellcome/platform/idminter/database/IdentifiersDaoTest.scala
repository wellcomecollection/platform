package uk.ac.wellcome.platform.idminter.database

import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.platform.idminter.fixtures.DatabaseConfig
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}
import uk.ac.wellcome.test.fixtures.TestWith

import scala.util.{Failure, Success}

class IdentifiersDaoTest
    extends FunSpec
    with fixtures.IdentifiersDatabase
    with Matchers {

  def withIdentifiersDao[R](
    testWith: TestWith[(IdentifiersDao, IdentifiersTable), R]): R =
    withIdentifiersDatabase { identifiersTableConfig =>
      val identifiersTable = new IdentifiersTable(identifiersTableConfig)

      new TableProvisioner(rdsClientConfig)
        .provision(
          database = identifiersTableConfig.database,
          tableName = identifiersTableConfig.tableName
        )

      val identifiersDao = new IdentifiersDao(DB.connect(), identifiersTable)

      eventuallyTableExists(identifiersTableConfig)

      testWith((identifiersDao, identifiersTable))
    }

  describe("lookupID") {
    it("gets an Identifier if it finds a matching SourceSystem and SourceId") {
      withIdentifiersDao {
        case (identifiersDao, _) =>
          val identifier = Identifier(
            CanonicalId = "A turtle turns to try to taste",
            SourceId = "A tangerine",
            SourceSystem = IdentifierType("miro-image-number").id,
            OntologyType = "t-t-t-turtles"
          )
          identifiersDao.saveIdentifier(identifier) shouldBe Success(1)

          val sourceIdentifier = SourceIdentifier(
            identifierType = IdentifierType("miro-image-number"),
            identifier.OntologyType,
            value = identifier.SourceId
          )

          val triedLookup = identifiersDao.lookupId(
            sourceIdentifier = sourceIdentifier
          )

          triedLookup shouldBe Success(Some(identifier))
      }
    }

    it(
      "does not get an identifier if there is no matching SourceSystem and SourceId") {
      withIdentifiersDao {
        case (identifiersDao, _) =>
          val identifier = Identifier(
            CanonicalId = "A turtle turns to try to taste",
            SourceId = "A tangerine",
            SourceSystem = IdentifierType("miro-image-number").id,
            OntologyType = "t-t-t-turtles"
          )

          identifiersDao.saveIdentifier(identifier) shouldBe Success(1)

          val sourceIdentifier = SourceIdentifier(
            identifierType = IdentifierType("miro-image-number"),
            identifier.OntologyType,
            value = "not_an_existing_value"
          )

          val triedLookup = identifiersDao.lookupId(
            sourceIdentifier = sourceIdentifier
          )

          triedLookup shouldBe Success(None)
      }
    }
  }

  describe("saveIdentifier") {
    it("inserts the provided identifier into the database") {
      withIdentifiersDao {
        case (identifiersDao, identifiersTable) =>
          implicit val session = AutoSession

          val identifier = Identifier(
            CanonicalId = "A provision of porpoises",
            OntologyType = "Work",
            SourceSystem = IdentifierType("miro-image-number").id,
            SourceId = "A picture of pangolins"
          )
          identifiersDao.saveIdentifier(identifier)
          val maybeIdentifier = withSQL {
            select
              .from(identifiersTable as identifiersTable.i)
              .where
              .eq(
                identifiersTable.i.SourceSystem,
                IdentifierType("miro-image-number").id)
              .and
              .eq(identifiersTable.i.CanonicalId, identifier.CanonicalId)
          }.map(Identifier(identifiersTable.i)).single.apply()

          maybeIdentifier shouldBe defined
          maybeIdentifier.get shouldBe identifier
      }
    }

    it("fails to insert a record with a duplicate CanonicalId") {
      withIdentifiersDao {
        case (identifiersDao, _) =>
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

          identifiersDao.saveIdentifier(identifier) shouldBe Success(1)

          val triedSave = identifiersDao.saveIdentifier(duplicateIdentifier)

          triedSave shouldBe a[Failure[_]]
          triedSave.failed.get shouldBe a[GracefulFailureException]
      }
    }

    it(
      "saves records with the same SourceSystem and SourceId but different OntologyType") {
      withIdentifiersDao {
        case (identifiersDao, _) =>
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

          identifiersDao.saveIdentifier(identifier) shouldBe Success(1)
          identifiersDao.saveIdentifier(secondIdentifier) shouldBe Success(1)
      }
    }

    it(
      "saves records with different SourceId but the same OntologyType and SourceSystem") {
      withIdentifiersDao {
        case (identifiersDao, _) =>
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

          identifiersDao.saveIdentifier(identifier) shouldBe Success(1)
          identifiersDao.saveIdentifier(secondIdentifier) shouldBe Success(1)
      }
    }

    it(
      "does not insert records with the same SourceId, SourceSystem and OntologyType") {
      withIdentifiersDao {
        case (identifiersDao, _) =>
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

          identifiersDao.saveIdentifier(identifier) shouldBe Success(1)

          val triedSave = identifiersDao.saveIdentifier(duplicateIdentifier)

          triedSave shouldBe a[Failure[_]]
          triedSave.failed.get shouldBe a[GracefulFailureException]
      }
    }
  }
}
