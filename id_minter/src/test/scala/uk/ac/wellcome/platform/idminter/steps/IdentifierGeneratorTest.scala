package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.IdentifiersMysqlLocal

import scala.concurrent.Future

class IdentifierGeneratorTest
    extends FunSpec
    with IdentifiersMysqlLocal
    with ScalaFutures
    with Matchers
    with BeforeAndAfterEach
    with MockitoSugar {

  private val metricsSender =
    new MetricsSender("id_minter_test_metrics", mock[AmazonCloudWatch])
  val identifierGenerator = new IdentifierGenerator(
    new IdentifiersDao(DB.connect(), identifiersTable),
    metricsSender)

  it(
    "should search the miro id in the database and return the canonical id if it finds it") {
    withSQL {
      insert
        .into(identifiersTable)
        .namedValues(identifiersTable.column.CanonicalID -> "5678",
                     identifiersTable.column.MiroID -> "1234",
                     identifiersTable.column.ontologyType -> "Work")
    }.update().apply()

    val work =
      Work(identifiers = List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "1234")),
           title = "Searching for a sea slug")
    val futureId = identifierGenerator.generateId(work)

    whenReady(futureId) { id =>
      id shouldBe "5678"
    }
  }

  it(
    "should generate an id and save it in the database if a record doesn't already exist") {
    val work =
      Work(identifiers = List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "1234")),
           title = "A novel name for a nightingale")
    val futureId = identifierGenerator.generateId(work)

    whenReady(futureId) { id =>
      id should not be (empty)
      val i = identifiersTable.i
      val maybeIdentifier = withSQL {
        select.from(identifiersTable as i).where.eq(i.MiroID, "1234")
      }.map(Identifier(i)).single.apply()
      maybeIdentifier shouldBe defined
      maybeIdentifier.get shouldBe Identifier(id, "1234")
    }
  }

  it("should reject an item with no miroId in the list of Identifiers") {
    val work =
      Work(
        identifiers = List(SourceIdentifier("not-a-miro-image-number", "1234")),
        title = "The rejection of a robin")
    val futureId = identifierGenerator.generateId(work)

    whenReady(futureId.failed) { exception =>
      exception.getMessage shouldBe s"Item $work did not contain a MiroID"
    }
  }

  it(
    "should return a failed future if it fails inserting the identifier in the database") {
    val miroId = "1234"
    val work =
      Work(identifiers = List(SourceIdentifier(IdentifierSchemes.miroImageNumber, miroId)),
           title = "A fear of failing in a fox")
    val identifiersDao = mock[IdentifiersDao]
    val identifierGenerator =
      new IdentifierGenerator(identifiersDao, metricsSender)

    when(identifiersDao.lookupMiroID(miroId))
      .thenReturn(Future.successful(None))
    val expectedException = new Exception("Noooo")
    when(identifiersDao.saveIdentifier(any[Identifier]()))
      .thenReturn(Future.failed(expectedException))

    whenReady(identifierGenerator.generateId(work).failed) { exception =>
      exception shouldBe expectedException
    }

  }
}
