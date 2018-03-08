package uk.ac.wellcome.dynamo

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}
import com.gu.scanamo.syntax._

class UpdateExpressionGeneratorTest extends FunSpec with Matchers {

  case class Example(id: String, data: String, number: Int, date: Instant)

  it("generates an update expression for a case class") {
    val example =
      Example(id = "111", data = "something", number = 1, date = Instant.now())

    val expectedUpdateExpression = set('data -> example.data) and set(
      'number -> example.number) and set('date -> example.date)

    val updateExpressionGenerator = UpdateExpressionGenerator[Example]

    updateExpressionGenerator.generateUpdateExpression(example) shouldBe Some(
      expectedUpdateExpression)
  }

  case class EmptyExample(id: String)

  it("returns none for a case class that contains only id") {
    val example = EmptyExample(id = "111")

    val updateExpressionGenerator = UpdateExpressionGenerator[EmptyExample]

    updateExpressionGenerator.generateUpdateExpression(example) shouldBe None
  }

}
