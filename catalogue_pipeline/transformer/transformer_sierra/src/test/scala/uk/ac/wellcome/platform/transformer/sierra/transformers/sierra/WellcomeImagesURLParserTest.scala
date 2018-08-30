package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra

import org.scalatest.{FunSpec, Matchers}

/** All of these test cases are based on real examples from the Sierra data. */
class WellcomeImagesURLParserTest extends FunSpec with Matchers {

  val miroIDexamples = List(
    "L0046161",
    "V0033167F1",
    "V0032544ECL"
  )

  it("indexplus/image URLs with a single '.html'") {
    miroIDexamples.foreach { miroID =>
      val url = s"http://wellcomeimages.org/indexplus/image/$miroID.html"
      assertURLParsedCorrectly(url, miroID = miroID)
    }
  }

  val transformer = new WellcomeImagesURLParser {}

  private def assertURLParsedCorrectly(url: String, miroID: String) = {
    val result = transformer.maybeGetMiroID(url)
    if (result.isEmpty) {
      println(s"$url did not return a Miro ID")
    }
    result shouldBe Some(miroID)
  }
}
