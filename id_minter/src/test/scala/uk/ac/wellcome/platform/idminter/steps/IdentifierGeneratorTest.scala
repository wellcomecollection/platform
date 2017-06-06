package uk.ac.wellcome.platform.idminter.steps

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.models.Identifiers.i
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{Identifier, Identifiers, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.MysqlLocal

class IdentifierGeneratorTest
    extends FunSpec
    with MysqlLocal
    with ScalaFutures
    with Matchers
    with BeforeAndAfterEach
    with IntegrationPatience {

  val identifierGenerator = new IdentifierGenerator(
    DB.connect(),
    DynamoConfig("applicationName", "streamArn", identifiersTableName))

  it("should search the miro id in dynamoDb and return the canonical id if it finds it") {
    withSQL {
      insert
        .into(Identifiers)
        .namedValues(Identifiers.column.CanonicalID -> "5678",
                     Identifiers.column.MiroID -> "1234")
    }.update().apply()

    val work =
      Work(identifiers = List(SourceIdentifier("Miro", "MiroID", "1234")),
           label = "some label")
    val futureId = identifierGenerator.generateId(work)

    whenReady(futureId) { id =>
      id shouldBe "5678"
    }
  }

  it("should generate an id and save it in the database if a record doesn't already exist") {
    val work =
      Work(identifiers = List(SourceIdentifier("Miro", "MiroID", "1234")),
           label = "some label")
    val futureId = identifierGenerator.generateId(work)

    whenReady(futureId) { id =>
      id should not be (empty)

      val maybeIdentifier = withSQL {
        select.from(Identifiers as i).where.eq(i.MiroID, "1234")
      }.map(Identifiers(i)).single.apply()
      maybeIdentifier shouldBe defined
      maybeIdentifier.get shouldBe Identifier(id, "1234")
    }
  }

  it("should reject an item with no miroId in the list of Identifiers") {
    val work =
      Work(
        identifiers = List(SourceIdentifier("NotMiro", "NotMiroID", "1234")),
        label = "some label")
    val futureId = identifierGenerator.generateId(work)

    whenReady(futureId.failed) { exception =>
      exception.getMessage shouldBe s"Item $work did not contain a MiroID"
    }
  }
}
