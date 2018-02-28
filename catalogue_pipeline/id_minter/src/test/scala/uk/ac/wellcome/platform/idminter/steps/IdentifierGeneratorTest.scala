package uk.ac.wellcome.platform.idminter.steps

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.Identifier
import uk.ac.wellcome.platform.idminter.utils.IdentifiersMysqlLocal

import scala.util.{Failure, Success}

class IdentifierGeneratorTest
    extends FunSpec
    with IdentifiersMysqlLocal
    with Matchers
    with MockitoSugar {

  private val metricsSender =
    new MetricsSender(
      "id_minter_test_metrics",
      mock[AmazonCloudWatch],
      ActorSystem())

  val identifierGenerator = new IdentifierGenerator(
    new IdentifiersDao(DB.connect(), identifiersTable),
    metricsSender
  )

  it("queries the database and return a matching canonical id") {
    withSQL {
      insert
        .into(identifiersTable)
        .namedValues(
          identifiersTable.column.CanonicalId -> "5678",
          identifiersTable.column.SourceSystem -> IdentifierSchemes.miroImageNumber.toString,
          identifiersTable.column.SourceId -> "1234",
          identifiersTable.column.OntologyType -> "Work"
        )
    }.update().apply()

    val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
      SourceIdentifier(IdentifierSchemes.miroImageNumber, "1234"),
      "Work"
    )

    triedId shouldBe Success("5678")
  }

  it("generates and saves a new identifier") {
    val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
      SourceIdentifier(IdentifierSchemes.miroImageNumber, "1234"),
      "Work"
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
      SourceSystem = IdentifierSchemes.miroImageNumber.toString,
      SourceId = "1234"
    )
  }

  it("returns a failure if it fails registering a new identifier") {

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

    val triedGeneratingId = identifierGenerator.retrieveOrGenerateCanonicalId(
      sourceIdentifier,
      "Work"
    )

    triedGeneratingId shouldBe a[Failure[Exception]]
    triedGeneratingId.failed.get shouldBe expectedException
  }

  it("should preserve the ontologyType when generating a new identifier") {
    val ontologyType = "Item"
    val miroId = "1234"

    val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
      SourceIdentifier(IdentifierSchemes.miroImageNumber, miroId),
      ontologyType
    )

    triedId shouldBe a[Success[String]]

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
      SourceSystem = IdentifierSchemes.miroImageNumber.toString,
      SourceId = miroId,
      OntologyType = ontologyType
    )
  }
}
