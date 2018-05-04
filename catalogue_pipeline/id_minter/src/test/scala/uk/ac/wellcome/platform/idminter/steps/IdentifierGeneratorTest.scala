package uk.ac.wellcome.platform.idminter.steps

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.models.work.internal.{
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.platform.idminter.database.{
  IdentifiersDao,
  TableProvisioner
}
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.platform.idminter.fixtures.DatabaseConfig
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class IdentifierGeneratorTest
    extends FunSpec
    with fixtures.IdentifiersDatabase
    with Matchers
    with MockitoSugar {

  private val metricsSender =
    new MetricsSender(
      "id_minter_test_metrics",
      100 milliseconds,
      mock[AmazonCloudWatch],
      ActorSystem())

  case class IdentifierGeneratorFixtures(
    identifierGenerator: IdentifierGenerator,
    identifiersTable: IdentifiersTable,
    dbConfig: DatabaseConfig
  )

  def withIdentifierGenerator[R](maybeIdentifiersDao: Option[IdentifiersDao] =
                                   None)(
    testWith: TestWith[IdentifierGeneratorFixtures, R]) =
    withIdentifiersDatabase[R] { dbConfig =>
      val identifiersTable: IdentifiersTable =
        new IdentifiersTable(dbConfig.databaseName, dbConfig.tableName)

      new TableProvisioner(host, port, username, password)
        .provision(dbConfig.databaseName, dbConfig.tableName)

      val identifiersDao = maybeIdentifiersDao.getOrElse(
        new IdentifiersDao(DB.connect(), identifiersTable)
      )

      val identifierGenerator = new IdentifierGenerator(
        identifiersDao,
        metricsSender
      )

      eventuallyTableExists(dbConfig)

      testWith(
        IdentifierGeneratorFixtures(
          identifierGenerator,
          identifiersTable,
          dbConfig))
    }

  it("queries the database and return a matching canonical id") {
    withIdentifierGenerator() { fixtures =>
      implicit val session = fixtures.dbConfig.session

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
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "Work", "1234")
      )

      triedId shouldBe Success("5678")
    }
  }

  it("generates and saves a new identifier") {
    withIdentifierGenerator() { fixtures =>
      implicit val session = fixtures.dbConfig.session

      val triedId = fixtures.identifierGenerator.retrieveOrGenerateCanonicalId(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "Work", "1234")
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
    val identifiersDao = mock[IdentifiersDao]
    val identifierGenerator = new IdentifierGenerator(
      identifiersDao,
      metricsSender
    )

    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
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

    withIdentifierGenerator(Some(identifiersDao)) { fixtures =>
      val triedGeneratingId =
        fixtures.identifierGenerator.retrieveOrGenerateCanonicalId(
          sourceIdentifier
        )

      triedGeneratingId shouldBe a[Failure[_]]
      triedGeneratingId.failed.get shouldBe expectedException
    }
  }

  it("should preserve the ontologyType when generating a new identifier") {
    withIdentifierGenerator() { fixtures =>
      implicit val session = fixtures.dbConfig.session

      val ontologyType = "Item"
      val miroId = "1234"

      val triedId = fixtures.identifierGenerator.retrieveOrGenerateCanonicalId(
        SourceIdentifier(
          IdentifierSchemes.miroImageNumber,
          ontologyType,
          miroId)
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
