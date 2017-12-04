package uk.ac.wellcome.transformer.parsers

import org.scalatest.FunSpec
import uk.ac.wellcome.models.MergedSierraRecord
import uk.ac.wellcome.models.transformable.Transformable

import scala.util.Try

class SierraParserTest extends FunSpec {
  it("should parse a sierra merged record") {
    val sierraParser = new SierraParser
  }
}

class SierraParser extends TransformableParser[MergedSierraRecord] {
  override def readFromRecord(message: String): Try[Transformable] = ???
}
