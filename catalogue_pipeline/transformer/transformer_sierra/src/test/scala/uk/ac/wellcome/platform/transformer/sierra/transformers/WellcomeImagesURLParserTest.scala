package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}

/** All of these test cases are based on real examples from the Sierra data. */
class WellcomeImagesURLParserTest extends FunSpec with Matchers {

  val miroIDexamples = List(
    "L0046161",
    "V0033167F1",
    "V0032544ECL"
  )

  describe("extracts Miro IDs for known URL patterns") {
    it("indexplus/image URLs with a single '.html'") {
      assertURLPatternParsedCorrectly { miroID =>
        s"http://wellcomeimages.org/indexplus/image/$miroID.html"
      }
    }

    it("indexplus/image URLs with a double '.html'") {
      assertURLPatternParsedCorrectly { miroID =>
        s"http://wellcomeimages.org/indexplus/image/$miroID.html.html"
      }
    }

    it("ixbin/hixclient URLs") {
      assertURLPatternParsedCorrectly { miroID =>
        s"http://wellcomeimages.org/ixbin/hixclient?MIROPAC=$miroID"
      }
    }

    it("ixbinixclient.exe URLs") {
      assertURLPatternParsedCorrectly { miroID =>
        s"http://wellcomeimages.org/ixbinixclient.exe?MIROPAC=$miroID.html.html"
      }
    }

    it("alternative ixbinixclient.exe URLs") {
      assertURLPatternParsedCorrectly { miroID =>
        s"http://wellcomeimages.org/ixbinixclient.exe?image=$miroID.html"
      }
    }
  }

  it("ignores URLs that are unrelated to Wellcome Images") {
    transformer.maybeGetMiroID(
      url =
        "http://film.wellcome.ac.uk:15151/mediaplayer.html?fug_7340-1&pw=524ph=600.html"
    ) shouldBe None
  }

  val transformer = new WellcomeImagesURLParser {}

  private def assertURLPatternParsedCorrectly(createURL: (String) => String) = {
    miroIDexamples.foreach { miroID =>
      val url = createURL(miroID)
      assertURLParsedCorrectly(url, miroID = miroID)
    }
  }

  private def assertURLParsedCorrectly(url: String, miroID: String) = {
    val result = transformer.maybeGetMiroID(url)
    if (result.isEmpty) {
      println(s"$url did not return a Miro ID")
    }
    result shouldBe Some(miroID)
  }
}
