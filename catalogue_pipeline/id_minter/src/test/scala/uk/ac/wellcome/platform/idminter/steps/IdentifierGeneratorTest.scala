package uk.ac.wellcome.platform.idminter.steps

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.idminter.database.{
  IdentifiersDao,
  TableProvisioner
}
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.util.{Failure, Success}

class IdentifierGeneratorTest
    extends FunSpec
    with Akka
    with fixtures.IdentifiersDatabase
    with MetricsSenderFixture
    with Matchers
    with MockitoSugar {

  def withIdentifierGenerator[R](maybeIdentifiersDao: Option[IdentifiersDao] =
                                   None)(
    testWith: TestWith[(IdentifierGenerator, IdentifiersTable), R]) =
    withIdentifiersDatabase[R] { identifiersTableConfig=>
      val identifiersTable = new IdentifiersTable(identifiersTableConfig)

      new TableProvisioner(rdsClientConfig)
        .provision(
          database = identifiersTableConfig.database,
          tableName = identifiersTableConfig.tableName
        )

      val identifiersDao = maybeIdentifiersDao.getOrElse(
        new IdentifiersDao(DB.connect(), identifiersTable)
      )

      withActorSystem { actorSystem =>
        withMetricsSender(actorSystem) { metricsSender =>
          val identifierGenerator = new IdentifierGenerator(
            identifiersDao,
            metricsSender
          )

          eventuallyTableExists(identifiersTableConfig)

          testWith((identifierGenerator, identifiersTable))
        }
      }
    }

  it("queries the database and return a matching canonical id") {
    withIdentifierGenerator() {
      case (identifierGenerator, identifiersTable) =>
        implicit val session = AutoSession

        withSQL {
          insert
            .into(identifiersTable)
            .namedValues(
              identifiersTable.column.CanonicalId -> "5678",
              identifiersTable.column.SourceSystem -> IdentifierType(
                "miro-image-number").id,
              identifiersTable.column.SourceId -> "1234",
              identifiersTable.column.OntologyType -> "Work"
            )
        }.update().apply()

        val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
          SourceIdentifier(
            identifierType = IdentifierType("miro-image-number"),
            "Work",
            "1234")
        )

        triedId shouldBe Success("5678")
    }
  }

  it("generates and saves a new identifier") {
    withIdentifierGenerator() {
      case (identifierGenerator, identifiersTable) =>
        implicit val session = AutoSession

        val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
          SourceIdentifier(
            identifierType = IdentifierType("miro-image-number"),
            "Work",
            "1234")
        )

        triedId shouldBe a[Success[_]]

        val id = triedId.get
        id should not be empty

        val i = identifiersTable.i

        val maybeIdentifier = withSQL {

          select
            .from(identifiersTable as i)
            .where
            .eq(i.SourceId, "1234")

        }.map(Identifier(i)).single.apply()

        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe Identifier(
          CanonicalId = id,
          SourceSystem = IdentifierType("miro-image-number").id,
          SourceId = "1234"
        )
    }
  }

  it("returns a failure if it fails registering a new identifier") {
    val identifiersDao = mock[IdentifiersDao]

    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        val sourceIdentifier = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          "Work",
          value = "1234"
        )

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
    }
  }

  it("should preserve the ontologyType when generating a new identifier") {
    withIdentifierGenerator() {
      case (identifierGenerator, identifiersTable) =>
        implicit val session = AutoSession

        val ontologyType = "Item"
        val miroId = "1234"

        val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
          SourceIdentifier(
            identifierType = IdentifierType("miro-image-number"),
            ontologyType,
            miroId)
        )

        val id = triedId.get
        id should not be (empty)

        val i = identifiersTable.i
        val maybeIdentifier = withSQL {

          select
            .from(identifiersTable as i)
            .where
            .eq(i.SourceId, miroId)

        }.map(Identifier(i)).single.apply()

        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe Identifier(
          CanonicalId = id,
          SourceSystem = IdentifierType("miro-image-number").id,
          SourceId = miroId,
          OntologyType = ontologyType
        )
    }
  }
}
