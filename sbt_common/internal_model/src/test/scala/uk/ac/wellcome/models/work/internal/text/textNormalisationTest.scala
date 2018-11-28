package uk.ac.wellcome.models.work.internal.text

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.text.TextNormalisation._

class textNormalisationTest
  extends FunSpec
    with Matchers {

  it("removes trailing character") {
      val examples = Table(
          ("in",        "out"),
          ("text",      "text"),
          ("text.",     "text"),
          (" text",     " text"),
          (" text.",    " text"),
          ("te xt.",     "te xt"),
          ("text . ",   "text"),
          ("text ,. ",   "text ,"),
          ("text.  ",   "text"),
          ("text..  ",  "text."),
          (".text",     ".text"),
          (".text.",    ".text"),
          (".text..",   ".text."),
          (".text. . ", ".text."),
          (".",         ""),
          ("..",        "."),
          ("a title ... with ... . ", "a title ... with ...")
        )
    forAll(examples) { (i: String, o: String) =>
      trimTrailing(i, '.') shouldBe o
    }
  }
}
