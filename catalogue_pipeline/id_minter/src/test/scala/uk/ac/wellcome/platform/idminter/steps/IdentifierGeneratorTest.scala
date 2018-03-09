package uk.ac.wellcome.platform.idminter.steps

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.platform.idminter.database.{IdentifiersDao, TableProvisioner}
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.util.{Failure, Success}

class IdentifierGeneratorTest
  extends FunSpec
    with fixtures.IdentifiersDatabase
    with Eventually
    with ExtendedPatience
    with Matchers
    with MockitoSugar {

  private val metricsSender =
    new MetricsSender(
      "id_minter_test_metrics",
      mock[AmazonCloudWatch],
      ActorSystem())

  case class IdentifierGeneratorFixtures(
                                        identifierGenerator: IdentifierGenerator,
                                        identifiersTable: IdentifiersTable
                                        )

  def withIdentifierGenerator[R](testWith: TestWith[IdentifierGeneratorFixtures, R]) = withIdentifiersDatabase[R] { dbConfig =>
    val identifiersTable: IdentifiersTable =
      new IdentifiersTable(dbConfig.databaseName, dbConfig.tableName)

    new TableProvisioner(host, port, username, password)
      .provision(dbConfig.databaseName, dbConfig.tableName)

    val identifierGenerator = new IdentifierGenerator(
      new IdentifiersDao(DB.connect(), identifiersTable),
      metricsSender
    )

    eventually {
      testWith(IdentifierGeneratorFixtures(identifierGenerator, identifiersTable))
    }
  }

  it("queries the database and return a matching canonical id") {
    withIdentifierGenerator { fixtures =>
      withSQL {
        insert
          .into(fixtures.identifiersTable)
          .namedValues(
            fixtures.identifiersTable.column.CanonicalId -> "5678",
            fixtures.identifiersTable.column.SourceSystem -> IdentifierSchemes.miroImageNumber.toString,
            fixtures.identifiersTable.column.SourceId -> "1234",
            fixtures.identifiersTable.column.OntologyType -> "Work"
          )
      }.update().apply()

      val triedId = fixtures.identifierGenerator.retrieveOrGenerateCanonicalId(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "1234"),
        "Work"
      )

      triedId shouldBe Success("5678")
    }
  }

  it("generates and saves a new identifier") {
    withIdentifierGenerator { fixtures =>

      val triedId = fixtures.identifierGenerator.retrieveOrGenerateCanonicalId(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "1234"),
        "Work"
      )

      triedId shouldBe a[Success[_]]

      val id = triedId.get
      id should not be empty

      val i = fixtures.identifiersTable.i

      val maybeIdentifier = withSQL {

        select
          .from(fixtures.identifiersTable as i)
          .where
          .eq(i.SourceId, "1234")

      }.map(Identifier(i)).single.apply()

      maybeIdentifier shouldBe defined
      maybeIdentifier.get shouldBe Identifier(
        CanonicalId = id,
        SourceSystem = IdentifierSchemes.miroImageNumber.toString,
        SourceId = "1234"
      )
    }
  }

  it("returns a failure if it fails registering a new identifier") {
    withIdentifierGenerator { fixtures =>
      val sourceIdentifier = SourceIdentifier(
        identifierScheme = IdentifierSchemes.miroImageNumber,
        value = "1234"
      )

      val identifiersDao = mock[IdentifiersDao]
      val identifierGenerator = new IdentifierGenerator(
        identifiersDao,
        metricsSender
      )

      val triedLookup = identifiersDao.lookupId(
        sourceIdentifier = sourceIdentifier,
        ontologyType = "Work"
      )

      when(triedLookup)
        .thenReturn(Success(None))

      val expectedException = new Exception("Noooo")

      when(identifiersDao.saveIdentifier(any[Identifier]()))
        .thenReturn(Failure(expectedException))

      val triedGeneratingId = fixtures.identifierGenerator.retrieveOrGenerateCanonicalId(
        sourceIdentifier,
        "Work"
      )

      triedGeneratingId shouldBe a[Failure[_]]
      triedGeneratingId.failed.get shouldBe expectedException
    }
  }

  it("should preserve the ontologyType when generating a new identifier") {
    withIdentifierGenerator { fixtures =>

      val ontologyType = "Item"
      val miroId = "1234"

      val triedId = fixtures.identifierGenerator.retrieveOrGenerateCanonicalId(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, miroId),
        ontologyType
      )

      val id = triedId.get
      id should not be (empty)

      val i = fixtures.identifiersTable.i
      val maybeIdentifier = withSQL {

        select
          .from(fixtures.identifiersTable as i)
          .where
          .eq(i.SourceId, miroId)

      }.map(Identifier(i)).single.apply()

      maybeIdentifier shouldBe defined
      maybeIdentifier.get shouldBe Identifier(
        CanonicalId = id,
        SourceSystem = IdentifierSchemes.miroImageNumber.toString,
        SourceId = miroId,
        OntologyType = ontologyType
      )
    }
  }
}
