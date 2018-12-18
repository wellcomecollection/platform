package uk.ac.wellcome.models.work.internal.text

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.text.TextNormalisation._

class textNormalisationTest extends FunSpec with Matchers {
  describe("trimTrailing") {
    it("removes trailing character") {
      val examples = Table(
        ("-in-", "-out-"),
        ("text", "text"),
        ("text.", "text"),
        (" text. ", " text"),
        ("text. ", "text"),
        ("text.  ", "text"),
        ("text . ", "text"),
        ("text.\t", "text"),
        ("text.\n", "text"),
        ("text.\r", "text"),
        ("text.\f", "text"),
        ("text .", "text"),
        (" text", " text"),
        (" \ttext.", " \ttext"),
        ("te xt.", "te xt"),
        ("text . ", "text"),
        ("text ,. ", "text ,"),
        ("text.  ", "text"),
        ("text..  ", "text."),
        (".text", ".text"),
        (".text.", ".text"),
        (".text..", ".text."),
        (".text. . ", ".text."),
        (".", ""),
        ("..", "."),
        ("a title ... with ... . ", "a title ... with ..."),
        ("a title ... with .... ", "a title ... with ...")
      )
      forAll(examples) { (i: String, o: String) =>
        trimTrailing(i, '.') shouldBe o
      }
    }

    it("removes trailing literal regexp character") {
      val examples = Table(
        ("-in-", "-char-"),
        ("text\\", '\\'),
        ("text^", '^')
      )
      forAll(examples) { (i: String, c: Char) =>
        trimTrailing(i, c) shouldBe "text"
      }
    }
  }

  describe("sentenceCase") {
    it("converts to sentence case") {
      val examples = Table(
        ("-in-", "-out-"),
        ("text", "Text"),
        ("TEXT", "TEXT"),
        ("teXT", "TeXT"),
        ("text text", "Text text"),
        ("Text Text", "Text Text"),
        ("Text teXT", "Text teXT")
      )
      forAll(examples) { (i: String, o: String) =>
        sentenceCase(i) shouldBe o
      }
    }
  }
}
