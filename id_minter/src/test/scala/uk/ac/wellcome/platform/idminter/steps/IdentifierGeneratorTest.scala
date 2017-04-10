package uk.ac.wellcome.platform.idminter.steps

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.{Identifier, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.test.utils.DynamoDBLocal

class IdentifierGeneratorTest
    extends FunSpec
    with DynamoDBLocal
    with ScalaFutures
    with Matchers
    with BeforeAndAfterEach
    with IntegrationPatience {

  val identifierGenerator = new IdentifierGenerator(dynamoDbClient)

  it("should search the miro id in dynamoDb and return the canonical id if it finds it") {
    Scanamo.put(dynamoDbClient)("Identifiers")(Identifier("5678", "1234"))

    val unifiedItem =
      UnifiedItem("id", List(SourceIdentifier("Miro", "MiroID", "1234")), None)
    val futureId = identifierGenerator.generateId(unifiedItem)

    whenReady(futureId) { id =>
      id shouldBe "5678"
    }
  }

  it("should generate an id and save it in the database if a record doesn't already exist") {
    val unifiedItem =
      UnifiedItem("id", List(SourceIdentifier("Miro", "MiroID", "1234")), None)
    val futureId = identifierGenerator.generateId(unifiedItem)

    whenReady(futureId) { id =>
      id should not be (empty)
      Scanamo.queryIndex[Identifier](dynamoDbClient)("Identifiers", "MiroID")(
        'MiroID -> "1234") shouldBe List(Right(Identifier(id, "1234")))
    }
  }

  it("should reject an item with no miroId in the list of Identifiers") {
    val unifiedItem =
      UnifiedItem("id", List(SourceIdentifier("NotMiro", "NotMiroID", "1234")), None)
    val futureId = identifierGenerator.generateId(unifiedItem)

    whenReady(futureId.failed) { exception =>
      exception.getMessage shouldBe s"Item $unifiedItem did not contain a MiroID"
    }
  }

  it("should return an error if it finds more than one record for the same MiroID") {
    val miroId = "1234"
    Scanamo.put(dynamoDbClient)("Identifiers")(Identifier("5678", miroId))
    Scanamo.put(dynamoDbClient)("Identifiers")(Identifier("8765", miroId))

    val unifiedItem =
      UnifiedItem("id", List(SourceIdentifier("Miro", "MiroID", miroId)), None)
    val futureId = identifierGenerator.generateId(unifiedItem)

    whenReady(futureId.failed) { exception =>
      exception.getMessage shouldBe s"Found more than one record with MiroID $miroId"
    }
  }
}
