package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.test.utils.MiroTransformableWrapper


/** Tests that the Miro transformer extracts the "title" field correctly.
 *
 *  The rules around this heuristic are somewhat fiddly, and we need to be
 *  careful that we're extracting the right fields from the Miro metadata.
 */
class MiroTransformableTitleTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("should use the image_title field on non-V records") {
    val title = "A picture of a parrot"
    transformRecordAndCheckTitle(
      data = s""""image_title": "$title"""",
      expectedTitle = title,
      miroCollection = "Images-A"
    )
  }


  it("""
    should use the title field in the V collection if the description field
    is absent
  """) {
    val title = "A limerick about a lemming"
    transformRecordAndCheckTitle(
      data = s""""image_title": "$title"""",
      expectedTitle = title,
      miroCollection = "Images-V"
    )
  }

  it("""
    should use the image_title field as the title on a V image if the
    image_title is not a prefix of image_image_desc
  """) {
    val title = "A tome about a turtle"
    val description = "A story of a starfish"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "$title",
        "image_image_desc": "$description"
      """,
      expectedTitle = title,
      expectedDescription = Some(description),
      miroCollection = "Images-V"
    )
  }

  it("""
    should use the first line of image_image_desc as the title on a V image
    if image_title is a prefix of said first line, and omit a description
    entirely (one-line description)
  """) {
    val title = "An icon of an iguana"
    val description = "An icon of an iguana is an intriguing image"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "$title",
        "image_image_desc": "$description"
      """,
      expectedTitle = description,
      expectedDescription = None,
      miroCollection = "Images-V"
    )
  }

  it("""
    should use the first line of image_image_desc as the title on a V image
    if image_title is a prefix of said first line (multi-line description)
  """) {
    val title = "An icon of an iguana"
    val longTitle = "An icon of an iguana is an intriguing image"
    val descriptionBody = "Woodcut, by A.R. Tist.  Italian.  1897."
    val description = s"$longTitle\\n\\n$descriptionBody"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "$title",
        "image_image_desc": "$description"
      """,
      expectedTitle = longTitle,
      expectedDescription = Some(descriptionBody),
      miroCollection = "Images-V"
    )
  }

  it("""
    should use the image_image_desc_academic if the image_image_desc field
    doesn't contain useful data (one-line description)
  """) {
    val academicDescription = "An alibi for an academic"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "-",
        "image_image_desc": "--",
        "image_image_desc_academic": "$academicDescription"
      """,
      expectedTitle = academicDescription
    )
  }

  it("""
    should use the image_image_desc_academic if the image_image_desc field
    doesn't contain useful data (single hyphen in the description)
  """) {
    val academicDescription = "Using an upside-down umbrella"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "-",
        "image_image_desc": "-",
        "image_image_desc_academic": "$academicDescription"
      """,
      expectedTitle = academicDescription
    )
  }

  it("""
    should use the image_image_desc_academic if the image_image_desc field
    doesn't contain useful data (double hyphen in title and description)
  """) {
    val academicDescription = "Dirty doubling of dastardly data"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "--",
        "image_image_desc": "--",
        "image_image_desc_academic": "$academicDescription"
      """,
      expectedTitle = academicDescription
    )
  }

  it("""
    should use the image_image_desc_academic if the image_image_desc field
    doesn't contain useful data (multi-line description)
  """) {
    val academicLabel = "A lithograph of a lecturer"
    val academicBody = "The corpus of a chancellor"
    val academicDescription = s"$academicLabel\\n\\n$academicBody"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "-",
        "image_image_desc": "--",
        "image_image_desc_academic": "$academicDescription"
      """,
      expectedTitle = academicLabel,
      expectedDescription = Some(academicBody)
    )
  }

  it("should use the description if image_title is None") {
    val description = "A noisy narwhal in November"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": null,
        "image_image_desc": "$description"
      """,
      expectedTitle = description,
      expectedDescription = None
    )
  }

  private def transformRecordAndCheckTitle(
    data: String,
    expectedTitle: String,
    expectedDescription: Option[String] = None,
    miroCollection: String = "TestCollection"
  ) = {
    val transformedWork = transformWork(
      MiroCollection = miroCollection,
      data = data
    )

    transformedWork.title shouldBe expectedTitle
    transformedWork.description shouldBe expectedDescription
  }
}


class MiroTransformableTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("should pass through the Miro identifier") {
    val MiroID = "M0000005_test"
    val work = transformWork(
      data = """"image_title": "A picture of a passing porpoise"""",
      MiroID = MiroID
    )
    work.identifiers shouldBe List(SourceIdentifier(IdentifierSchemes.miroImageNumber, MiroID))
  }

  describe("The INNOPAC ID should be passed through as the Sierra system number") {
    it("plain numeric ID") {
      transformRecordAndCheckSierraSystemNumber(
        innopacId = "12345678",
        expectedSierraNumber = "b1234567"
      )
    }

    it("with an x for a check digit") {
      transformRecordAndCheckSierraSystemNumber(
        innopacId = "1234567x",
        expectedSierraNumber = "b1234567"
      )
    }

    it("with a leading b on the b-number") {
      transformRecordAndCheckSierraSystemNumber(
        innopacId = "b12345678",
        expectedSierraNumber = "b1234567"
      )
    }

    it("with a leading B on the b-number") {
      transformRecordAndCheckSierraSystemNumber(
        innopacId = "b12345678",
        expectedSierraNumber = "b1234567"
      )
    }

    it("with a leading .b on the b-number") {
      transformRecordAndCheckSierraSystemNumber(
        innopacId = ".b12345678",
        expectedSierraNumber = "b1234567"
      )
    }
  }

  it("should have an empty list if no image_creator field is present") {
    val work = transformWork(data = s""""image_title": "A guide to giraffes"""")
    work.creators shouldBe List[Agent]()
  }

  it("should have an empty list if the image_creator field is empty") {
    val work = transformWork(data =
      s"""
      "image_title": "A box of beavers",
      "image_creator": []
    """)
    work.creators shouldBe List[Agent]()
  }

  it("should pass through a single value in the image_creator field") {
    val creator = "Researcher Rosie"
    val work = transformWork(
      data =
        s"""
        "image_title": "A radio for a racoon",
        "image_creator": ["$creator"]
      """
    )
    work.creators shouldBe List(Agent(creator))
  }

  it("should pass through multiple values in the image_creator field") {
    val creator1 = "Beekeeper Brian"
    val creator2 = "Cat-wrangler Carol"
    val creator3 = "Dog-owner Derek"
    val work = transformWork(
      data =
        s"""
        "image_title": "A book about badgers",
        "image_creator": ["$creator1", "$creator2", "$creator3"]
      """
    )
    work.creators shouldBe List(Agent(creator1),
      Agent(creator2),
      Agent(creator3))
  }

  it("should have no description if no image_image_desc field is present") {
    val work = transformWork(data = s""""image_title": "A line of lions"""")
    work.description shouldBe None
  }

  it("should pass through the value of the description field") {
    val description = "A new novel about northern narwhals in November"
    val work = transformWork(
      data =
        s"""
        "image_title": "A note on narwhals",
        "image_image_desc": "$description"
      """
    )
    work.description shouldBe Some(description)
  }

  it("should pass through the value of the creation date on V records") {
    val date = "1820-1848"
    val work = transformWork(
      data =
        s"""
        "image_title": "A description of a dalmation",
        "image_image_desc": "A description of a dalmation with dots",
        "image_artwork_date": "$date"
      """,
      MiroCollection = "Images-V"
    )
    work.createdDate shouldBe Some(Period(date))
  }

  it("should not pass through the value of the creation date on non-V records") {
    val date = "1820-1848"
    val work = transformWork(
      data =
        s"""
        "image_title": "A diary about a dodo",
        "image_artwork_date": "$date"
      """,
      MiroCollection = "Images-A"
    )
    work.createdDate shouldBe None
  }

  it(
    "should use the image_creator_secondary field if image_creator is not present") {
    val secondaryCreator = "Scientist Sarah"
    val work = transformWork(
      data =
        s"""
        "image_title": "Samples of a shark",
        "image_secondary_creator": ["$secondaryCreator"]
      """
    )
    work.creators shouldBe List(Agent(secondaryCreator))
  }

  it(
    "should use all the values in the image_creator_secondary field if image_creator is not present") {
    val secondaryCreator1 = "Gamekeeper Gordon"
    val secondaryCreator2 = "Herpetologist Harriet"
    val work = transformWork(
      data =
        s"""
        "image_title": "Verdant and vivid",
        "image_secondary_creator": ["$secondaryCreator1", "$secondaryCreator2"]
      """
    )
    work.creators shouldBe List(Agent(secondaryCreator1),
      Agent(secondaryCreator2))
  }

  it(
    "should combine the values in the image_creator and image_secondary_creator fields if both present") {
    val creator = "Mycologist Morgan"
    val secondaryCreator = "Manufacturer Mel"
    val work = transformWork(
      data =
        s"""
        "image_title": "Musings on mice",
        "image_creator": ["$creator"],
        "image_secondary_creator": ["$secondaryCreator"]
      """
    )
    work.creators shouldBe List(Agent(creator), Agent(secondaryCreator))
  }

  it("should pass through the lettering field if available") {
    val lettering = "A lifelong lament for lemurs"
    val work = transformWork(
      data =
        s"""
        "image_title": "Lemurs and lemons",
        "image_supp_lettering": "$lettering"
      """
    )
    work.lettering shouldBe Some(lettering)
  }

  it(
    "should correct HTML-encoded entities in the input JSON") {
    val work = transformWork(
      data =
        s"""
        "image_title": "A caf&#233; for cats",
        "image_creator": ["Gyokush&#333;, a c&#228;t &#212;wn&#234;r"]
      """
    )

    work.title shouldBe "A café for cats"
    work.creators shouldBe List(Agent("Gyokushō, a cät Ôwnêr"))
  }

  private def transformRecordAndCheckSierraSystemNumber(
    innopacId: String,
    expectedSierraNumber: String,
    miroID: String = "V0000832"
  ) = {
    val work = transformWork(
      data = s"""
        "image_title": "A bouncing bundle of bison",
        "image_innopac_id": "$innopacId"
      """,
      MiroID = miroID
    )
    work.identifiers shouldBe List(
      SourceIdentifier(IdentifierSchemes.miroImageNumber, miroID),
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, expectedSierraNumber)
    )
  }
}

/** Tests that the Miro transformer extracts the "subjects" field correctly.
 *
 *  Although this transformation is currently a bit basic, the data we get
 *  from Miro will need cleaning before it's presented in the API (casing,
 *  names, etc.) -- these tests will become more complicated.
 */
class MiroTransformableSubjectsTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("should have an empty subject list on records without keywords") {
    transformRecordAndCheckSubjects(
      data = s""""image_title": "A snail without a subject"""",
      expectedSubjects = List[Concept]()
    )
  }

  it("should use the image_keywords field if present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A scorpion with a strawberry",
        "image_keywords": ["animals", "arachnids", "fruit"]
      """,
      expectedSubjects = List(
        Concept("animals"), Concept("arachnids"), Concept("fruit")
      )
    )
  }

  it("should use the image_keywords_unauth field if present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A sweet seal gives you a sycamore",
        "image_keywords_unauth": ["altruism", "mammals"]
      """,
      expectedSubjects = List(
        Concept("altruism"), Concept("mammals")
      )
    )
  }

  it("should use the image_keywords and image_keywords_unauth fields if both present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A squid, a sponge and a stingray walk into a bar",
        "image_keywords": ["humour"],
        "image_keywords_unauth": ["marine creatures"]
      """,
      expectedSubjects = List(
        Concept("humour"), Concept("marine creatures")
      )
    )
  }

  it("should create an Item for each Work") {
    val title = "A woodcut of a Weevil"
    val longTitle = "A wonderful woodcut of a weird weevil"
    val descriptionBody = "Woodcut, by A.R. Thropod.  Welsh.  1789."
    val description = s"$longTitle\\n\\n$descriptionBody"
    val work = transformWork(
      data = s"""
        "image_title": "$title",
        "image_image_desc": "$description"
      """)

    val item = work.items.head

    item shouldBe Item(
      None,
      List(
        SourceIdentifier("miro-image-number","M0000001")
      ),
      List(
        Location(
          locationType = "iiif-image",
          url = Some("https://iiif.wellcomecollection.org/image/M0000001.jpg/info.json"),
          license = License_CCBY
        )
      )
    )
  }

  private def transformRecordAndCheckSubjects(
    data: String,
    expectedSubjects: List[Concept] = List()
  ) = {
    val transformedWork = transformWork(data = data)
    transformedWork.subjects shouldBe expectedSubjects
  }
}



class MiroTransformableGenresTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("should have an empty genre list on records without keywords") {
    transformRecordAndCheckGenres(
      data = s""""image_title": "The giraffe's genre is gone'"""",
      expectedGenres = List[Concept]()
    )
  }

  it("should use the image_phys_format field if present") {
    transformRecordAndCheckGenres(
      data = s"""
        "image_title": "A goat grazes on some grass",
        "image_phys_format": "painting"
      """,
      expectedGenres = List(
        Concept("painting")
      )
    )
  }

  it("should use the image_lc_genre field if present") {
    transformRecordAndCheckGenres(
      data = s"""
        "image_title": "Grouchy geese are good as guards",
        "image_lc_genre": "sculpture"
      """,
      expectedGenres = List(
        Concept("sculpture")
      )
    )
  }

  it("should use the image_phys_format and image_lc_genre fields if both present") {
    transformRecordAndCheckGenres(
      data = s"""
        "image_title": "A gorilla and a gibbon in a garden",
        "image_phys_format": "etching",
        "image_lc_genre": "woodwork"
      """,
      expectedGenres = List(
        Concept("etching"), Concept("woodwork")
      )
    )
  }

  it("should deduplicate entries in the genre field if necessary") {
    transformRecordAndCheckGenres(
      data = s"""
        "image_title": "A duality of dancing dodos",
        "image_phys_format": "oil painting",
        "image_lc_genre": "oil painting"
      """,
      expectedGenres = List(
        Concept("oil painting")
      )
    )
  }

  private def transformRecordAndCheckGenres(
    data: String,
    expectedGenres: List[Concept] = List()
  ) = {
    val transformedWork = transformWork(data = data)
    transformedWork.genres shouldBe expectedGenres
  }
}



class MiroTransformableCopyrightTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("should have no copyright line if there's not enough information") {
    transformRecordAndCheckCopyright(
      data = s""""image_title": "An image without any copyright?""""
    )
  }

  it("should use the image_credit_line field if present") {
    transformRecordAndCheckCopyright(
      data = s"""
        "image_title": "A tumultuous transformation of trees",
        "image_credit_line": "Wellcome Collection"
      """,
      expectedCopyright = Some("Wellcome Collection")
    )
  }

  it("should use the image_credit_line in preference to image_source_code") {
    transformRecordAndCheckCopyright(
      data = s"""
        "image_title": "A tumultuous transformation of trees",
        "image_credit_line": "Wellcome Collection",
        "image_source_code": "CAM"
      """,
      expectedCopyright = Some("Wellcome Collection")
    )
  }

  it("should use image_source_code if image_credit_line is empty") {
    transformRecordAndCheckCopyright(
      data = s"""
        "image_title": "A tumultuous transformation of trees",
        "image_credit_line": null,
        "image_source_code": "CAM"
      """,
      expectedCopyright = Some("Benedict Campbell")
    )
  }


  it("should use the uppercased version of the source_code if necessary") {
    transformRecordAndCheckCopyright(
      data = s"""
        "image_title": "A loud and leafy lime",
        "image_source_code": "wel"
      """,
      expectedCopyright = Some("Wellcome Collection")
    )
  }

  it("should tidy up the credit line if necessary") {
    transformRecordAndCheckCopyright(
      data = s"""
        "image_title": "Outside an odorous oak",
        "image_credit_line": "The Wellcome Library, London"
      """,
      expectedCopyright = Some("Wellcome Collection")
    )
  }

  private def transformRecordAndCheckCopyright(
    data: String,
    expectedCopyright: Option[String] = None
  ) = {
    val transformedWork = transformWork(data = data)
    transformedWork.items.head.locations.head.copyright shouldBe expectedCopyright
  }
}
