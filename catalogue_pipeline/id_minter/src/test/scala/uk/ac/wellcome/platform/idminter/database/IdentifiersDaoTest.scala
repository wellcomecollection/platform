package uk.ac.wellcome.platform.idminter.database

import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil
import uk.ac.wellcome.platform.idminter.database.exceptions.IdMinterException
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}
import uk.ac.wellcome.test.fixtures.TestWith

import scala.util.{Failure, Success}

class IdentifiersDaoTest
    extends FunSpec
    with fixtures.IdentifiersDatabase
    with Matchers
    with IdentifiersUtil {

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
      val sourceIdentifier = createSourceIdentifier
      val identifier = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = sourceIdentifier
      )

      withIdentifiersDao {
        case (identifiersDao, _) =>
          identifiersDao.saveIdentifier(identifier) shouldBe Success(1)

          val triedLookup = identifiersDao.lookupId(
            sourceIdentifier = sourceIdentifier
          )

          triedLookup shouldBe Success(Some(identifier))
      }
    }

    it(
      "does not get an identifier if there is no matching SourceSystem and SourceId") {
      val identifier = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = createSourceIdentifier
      )

      withIdentifiersDao {
        case (identifiersDao, _) =>
          identifiersDao.saveIdentifier(identifier) shouldBe Success(1)

          val unknownSourceIdentifier = createSourceIdentifierWith(
            ontologyType = identifier.OntologyType,
            value = "not_an_existing_value"
          )

          val triedLookup = identifiersDao.lookupId(
            sourceIdentifier = unknownSourceIdentifier
          )

          triedLookup shouldBe Success(None)
      }
    }
  }

  describe("saveIdentifier") {
    it("inserts the provided identifier into the database") {
      val identifier = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = createSourceIdentifier
      )

      withIdentifiersDao {
        case (identifiersDao, identifiersTable) =>
          implicit val session = AutoSession

          identifiersDao.saveIdentifier(identifier)
          val maybeIdentifier = withSQL {
            select
              .from(identifiersTable as identifiersTable.i)
              .where
              .eq(identifiersTable.i.SourceSystem, identifier.SourceSystem)
              .and
              .eq(identifiersTable.i.CanonicalId, identifier.CanonicalId)
          }.map(Identifier(identifiersTable.i)).single.apply()

          maybeIdentifier shouldBe defined
          maybeIdentifier.get shouldBe identifier
      }
    }

    it("fails to insert a record with a duplicate CanonicalId") {
      val identifier = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = createSourceIdentifier
      )
      val duplicateIdentifier = Identifier(
        canonicalId = identifier.CanonicalId,
        sourceIdentifier = createSourceIdentifier
      )

      withIdentifiersDao {
        case (identifiersDao, _) =>
          identifiersDao.saveIdentifier(identifier) shouldBe Success(1)

          val triedSave = identifiersDao.saveIdentifier(duplicateIdentifier)

          triedSave shouldBe a[Failure[_]]
          triedSave.failed.get shouldBe a[IdMinterException]
      }
    }

    it(
      "saves records with the same SourceSystem and SourceId but different OntologyType") {
      val sourceIdentifier1 = createSourceIdentifierWith(
        ontologyType = "Foo"
      )
      val sourceIdentifier2 = createSourceIdentifierWith(
        ontologyType = "Bar"
      )

      val identifier1 = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = sourceIdentifier1
      )

      val identifier2 = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = sourceIdentifier2
      )

      withIdentifiersDao {
        case (identifiersDao, _) =>
          identifiersDao.saveIdentifier(identifier1) shouldBe Success(1)
          identifiersDao.saveIdentifier(identifier2) shouldBe Success(1)
      }
    }

    it(
      "saves records with different SourceId but the same OntologyType and SourceSystem") {
      val sourceIdentifier1 = createSourceIdentifierWith(
        value = "1234"
      )
      val sourceIdentifier2 = createSourceIdentifierWith(
        value = "5678"
      )

      val identifier1 = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = sourceIdentifier1
      )

      val identifier2 = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = sourceIdentifier2
      )

      withIdentifiersDao {
        case (identifiersDao, _) =>
          identifiersDao.saveIdentifier(identifier1) shouldBe Success(1)
          identifiersDao.saveIdentifier(identifier2) shouldBe Success(1)
      }
    }

    it(
      "does not insert records with the same SourceId, SourceSystem and OntologyType") {
      val sourceIdentifier = createSourceIdentifier

      val identifier1 = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = sourceIdentifier
      )
      val identifier2 = Identifier(
        canonicalId = createCanonicalId,
        sourceIdentifier = sourceIdentifier
      )

      withIdentifiersDao {
        case (identifiersDao, _) =>
          identifiersDao.saveIdentifier(identifier1) shouldBe Success(1)

          val triedSave = identifiersDao.saveIdentifier(identifier2)

          triedSave shouldBe a[Failure[_]]
          triedSave.failed.get shouldBe a[IdMinterException]
      }
    }
  }
}
