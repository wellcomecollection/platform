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

  it("should throw an error if there isn't a title field") {
    assertTransformWorkFails(data = """{
      "image_cleared": "Y",
      "image_copyright_cleared": "Y"
    }""")
  }

  it("should pass through the Miro identifier") {
    val MiroID = "M0000005_test"
    val work = transformWork(
      data = """"image_title": "A picture of a passing porpoise"""",
      MiroID = MiroID
    )
    work.identifiers shouldBe List(SourceIdentifier(IdentifierSchemes.miroImageNumber, MiroID))
  }

  it("should have an empty list if no image_creator field is present") {
    val work = transformWork(data = s""""image_title": "A guide to giraffes"""")
    work.creators shouldBe List[Agent]()
  }

  it("should have an empty list if the image_creator field is empty") {
    val work = transformWork(data = s"""
      "image_title": "A box of beavers",
      "image_creator": []
    """)
    work.creators shouldBe List[Agent]()
  }

  it("should pass through a single value in the image_creator field") {
    val creator = "Researcher Rosie"
    val work = transformWork(
      data = s"""
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
      data = s"""
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
      data = s"""
        "image_title": "A note on narwhals",
        "image_image_desc": "$description"
      """
    )
    work.description shouldBe Some(description)
  }

  it("should pass through the value of the creation date on V records") {
    val date = "1820-1848"
    val work = transformWork(
      data = s"""
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
      data = s"""
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
      data = s"""
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
      data = s"""
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
      data = s"""
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
      data = s"""
        "image_title": "Lemurs and lemons",
        "image_supp_lettering": "$lettering"
      """
    )
    work.lettering shouldBe Some(lettering)
  }

  it(
    "should correct HTML-encoded entities in the input JSON") {
    val work = transformWork(
      data = s"""
        "image_title": "A caf&#233; for cats",
        "image_creator": ["Gyokush&#333;, a c&#228;t &#212;wn&#234;r"]
      """
    )

    work.title shouldBe "A café for cats"
    work.creators shouldBe List(Agent("Gyokushō, a cät Ôwnêr"))
  }

  it("should not pass through records with a missing image_cleared field") {
    assertTransformWorkFails(data = """{
      "image_title": "Missives on museums",
      "image_copyright_cleared": "Y"
    }""")
  }

  it(
    "should not pass through records with a missing image_copyright_cleared field") {
    assertTransformWorkFails(
      data = """{
      "image_title": "A caricature of cats",
      "image_cleared": "Y"
    }""")
  }

  it(
    "should not pass through records with missing image_cleared and missing image_copyright_cleared field") {
    assertTransformWorkFails(
      data = """{
      "image_title": "Drawings of dromedaries"
    }""")
  }

  it(
    "should not pass through records with an image_cleared value that isn't 'Y'") {
    assertTransformWorkFails(
      data = """{
      "image_title": "Confidential colourings of crocodiles",
      "image_cleared": "N",
      "image_copyright_cleared": "Y"
    }""")
  }

  it(
    "should not pass through records with image_copyright_cleared field that isn't 'Y'") {
    assertTransformWorkFails(
      data = """{
      "image_title": "Proprietary poetry about porcupines",
      "image_cleared": "Y",
      "image_copyright_cleared": "N"
    }""")
  }

  it(
    "should not pass through records that are missing technical metadata") {
    assertTransformWorkFails(
      data = """{
        "image_title": "Touching a toxic tree is truly tragic",
        "image_cleared": "Y",
        "image_copyright_cleared": "Y",
        "image_tech_file_size": []
      }"""
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

  private def transformRecordAndCheckGenres(
    data: String,
    expectedGenres: List[Concept] = List()
  ) = {
    val transformedWork = transformWork(data = data)
    transformedWork.genres shouldBe expectedGenres
  }
}



class MiroTransformableThumbnailTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("should reject records that don't have usage data") {
    assertTransformWorkFails(
      data = """{
        "image_cleared": "Y",
        "image_copyright_cleared": "Y",
        "image_tech_file_size": ["1000000"],
        "image_title": "Understand that using this umbrella is unauthorised"
      }"""
    )
  }

  it("should reject records with unrecognised usage data") {
    assertTransformWorkFails(
      data = """{
        "image_cleared": "Y",
        "image_copyright_cleared": "Y",
        "image_tech_file_size": ["1000000"],
        "image_use_restrictions": "Poetic license, normally reserved for playwrights and not suitable in practice",
        "image_title": "Plagiarised poetry by a penguin"
      }"""
    )
  }

  it("should create a thumbnail if the license is present") {
    transformRecordAndCheckThumbnail(
      data = s"""
        "image_use_restrictions": "CC-BY-NC",
        "image_title": "A thumb-sized tarantula"
      """,
      MiroID = "MT0001234",
      expectedThumbnail = Location(
        locationType = "thumbnail-image",
        url = Some("https://iiif.wellcomecollection.org/image/MT0001234.jpg/full/300,/0/default.jpg"),
        license = License_CCBYNC
      )
    )
  }

  private def transformRecordAndCheckThumbnail(
    data: String,
    MiroID: String,
    expectedThumbnail: Location
  ) = {
    val transformedWork = transformWork(data = data, MiroID = MiroID)
    transformedWork.thumbnail.get shouldBe expectedThumbnail
  }
}
