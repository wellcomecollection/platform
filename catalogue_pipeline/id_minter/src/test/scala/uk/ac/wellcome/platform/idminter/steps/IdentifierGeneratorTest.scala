package uk.ac.wellcome.platform.idminter.steps

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.platform.idminter.database.{
  IdentifiersDao,
  TableProvisioner
}
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}
import uk.ac.wellcome.fixtures.TestWith

import scala.util.{Failure, Success}

class IdentifierGeneratorTest
    extends FunSpec
    with fixtures.IdentifiersDatabase
    with Matchers
    with MockitoSugar
    with IdentifiersGenerators {

  def withIdentifierGenerator[R](maybeIdentifiersDao: Option[IdentifiersDao] =
                                   None)(
    testWith: TestWith[(IdentifierGenerator, IdentifiersTable), R]) =
    withIdentifiersDatabase[R] { identifiersTableConfig =>
      val identifiersTable = new IdentifiersTable(identifiersTableConfig)

      new TableProvisioner(rdsClientConfig)
        .provision(
          database = identifiersTableConfig.database,
          tableName = identifiersTableConfig.tableName
        )

      val identifiersDao = maybeIdentifiersDao.getOrElse(
        new IdentifiersDao(DB.connect(), identifiersTable)
      )

      val identifierGenerator = new IdentifierGenerator(identifiersDao)
      eventuallyTableExists(identifiersTableConfig)

      testWith((identifierGenerator, identifiersTable))
    }

  it("queries the database and return a matching canonical id") {
    val sourceIdentifier = createSourceIdentifier
    val canonicalId = createCanonicalId

    withIdentifierGenerator() {
      case (identifierGenerator, identifiersTable) =>
        implicit val session = AutoSession

        withSQL {
          insert
            .into(identifiersTable)
            .namedValues(
              identifiersTable.column.CanonicalId -> canonicalId,
              identifiersTable.column.SourceSystem -> sourceIdentifier.identifierType.id,
              identifiersTable.column.SourceId -> sourceIdentifier.value,
              identifiersTable.column.OntologyType -> sourceIdentifier.ontologyType
            )
        }.update().apply()

        val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
          sourceIdentifier
        )

        triedId shouldBe Success(canonicalId)
    }
  }

  it("generates and saves a new identifier") {
    val sourceIdentifier = createSourceIdentifier

    withIdentifierGenerator() {
      case (identifierGenerator, identifiersTable) =>
        implicit val session = AutoSession

        val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
          sourceIdentifier
        )

        triedId shouldBe a[Success[_]]

        val id = triedId.get
        id should not be empty

        val i = identifiersTable.i

        val maybeIdentifier = withSQL {

          select
            .from(identifiersTable as i)
            .where
            .eq(i.SourceId, sourceIdentifier.value)

        }.map(Identifier(i)).single.apply()

        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe Identifier(
          canonicalId = id,
          sourceIdentifier = sourceIdentifier
        )
    }
  }

  it("returns a failure if it fails registering a new identifier") {
    val identifiersDao = mock[IdentifiersDao]

    val sourceIdentifier = createSourceIdentifier

    val triedLookup = identifiersDao.lookupId(
      sourceIdentifier = sourceIdentifier
    )

    when(triedLookup)
      .thenReturn(Success(None))

    val expectedException = new Exception("Noooo")

    when(identifiersDao.saveIdentifier(any[Identifier]()))
      .thenReturn(Failure(expectedException))

    withIdentifierGenerator(Some(identifiersDao)) {
      case (identifierGenerator, identifiersTable) =>
        val triedGeneratingId =
          identifierGenerator.retrieveOrGenerateCanonicalId(
            sourceIdentifier
          )

        triedGeneratingId shouldBe a[Failure[_]]
        triedGeneratingId.failed.get shouldBe expectedException
    }
  }

  it("preserves the ontologyType when generating a new identifier") {
    withIdentifierGenerator() {
      case (identifierGenerator, identifiersTable) =>
        implicit val session = AutoSession

        val sourceIdentifier = createSourceIdentifierWith(
          ontologyType = "Item"
        )

        val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
          sourceIdentifier
        )

        val id = triedId.get
        id should not be (empty)

        val i = identifiersTable.i
        val maybeIdentifier = withSQL {

          select
            .from(identifiersTable as i)
            .where
            .eq(i.SourceId, sourceIdentifier.value)

        }.map(Identifier(i)).single.apply()

        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe Identifier(
          canonicalId = id,
          sourceIdentifier = sourceIdentifier
        )
    }
  }
}
