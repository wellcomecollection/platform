package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

class MiroTransformableTest extends FunSpec with Matchers {

  it("should throw an error if there isn't a title field") {
    assertTransformMiroRecordFails(data="""{
      "image_cleared": "Y",
      "image_copyright_cleared": "Y"
    }""")
  }

  it("should pass through the Miro identifier") {
    val miroID = "M0000005_test"
    val work = transformMiroRecord(miroID = miroID)
    work.identifiers shouldBe List(SourceIdentifier("Miro", "MiroID", miroID))
  }

  it("should pass through the image_title to the label field") {
    val title = "A picture of a parrot"
    val work = transformMiroRecord(data = s"""{
      "image_title": "$title",
      "image_cleared": "Y",
      "image_copyright_cleared": "Y"
    }""")
    work.label shouldBe title
  }

  it("should have an empty list if no image_creator field is present") {
    val work = transformMiroRecord(data = s"""{
      "image_title": "A guide to giraffes",
      "image_cleared": "Y",
      "image_copyright_cleared": "Y"
    }""")
    work.creators shouldBe List[Agent]()
  }

  it("should have an empty list if the image_creator field is empty") {
    val work = transformMiroRecord(data = s"""{
      "image_title": "A box of beavers",
      "image_creator": [],
      "image_cleared": "Y",
      "image_copyright_cleared": "Y"
    }""")
    work.creators shouldBe List[Agent]()
  }

  it("should pass through a single value in the image_creator field") {
    val creator = "Researcher Rosie"
    val work = transformMiroRecord(
      data = s"""{
        "image_title": "A radio for a racoon",
        "image_creator": ["$creator"],
        "image_cleared": "Y",
        "image_copyright_cleared": "Y"
      }"""
    )
    work.creators shouldBe List(Agent(creator))
  }

  it("should pass through multiple values in the image_creator field") {
    val creator1 = "Beekeeper Brian"
    val creator2 = "Cat-wrangler Carol"
    val creator3 = "Dog-owner Derek"
    val work = transformMiroRecord(
      data = s"""{
        "image_title": "A book about badgers",
        "image_creator": ["$creator1", "$creator2", "$creator3"],
        "image_cleared": "Y",
        "image_copyright_cleared": "Y"
      }"""
    )
    work.creators shouldBe List(Agent(creator1), Agent(creator2), Agent(creator3))
  }

  it("should have no description if no image_image_desc field is present") {
    val work = transformMiroRecord(data = s"""{
      "image_title": "A line of lions",
      "image_cleared": "Y",
      "image_copyright_cleared": "Y"
    }""")
    work.description shouldBe None
  }

  it("should pass through the value of the description field") {
    val description = "A new novel about northern narwhals in November"
    val work = transformMiroRecord(
      data = s"""{
        "image_title": "A note on narwhals",
        "image_image_desc": "$description",
        "image_cleared": "Y",
        "image_copyright_cleared": "Y"
      }"""
    )
    work.description shouldBe Some(description)
  }

  it("should pass through the value of the creation date on V records") {
    val date = "1820-1848"
    val work = transformMiroRecord(
      data = s"""{
        "image_title": "A description of a dalmation",
        "image_artwork_date": "$date",
        "image_cleared": "Y",
        "image_copyright_cleared": "Y"
      }""",
      miroCollection = "Images-V"
    )
    work.createdDate shouldBe Some(Period(date))
  }

  it("should not pass through the value of the creation date on non-V records") {
    val date = "1820-1848"
    val work = transformMiroRecord(
      data = s"""{
        "image_title": "A diary about a dodo",
        "image_artwork_date": "$date",
        "image_cleared": "Y",
        "image_copyright_cleared": "Y"
      }""",
      miroCollection = "Images-A"
    )
    work.createdDate shouldBe None
  }

  it("should use the image_creator_secondary field if image_creator is not present") {
    val secondaryCreator = "Scientist Sarah"
    val work = transformMiroRecord(
      data = s"""{
        "image_title": "Samples of a shark",
        "image_secondary_creator": ["$secondaryCreator"],
        "image_cleared": "Y",
        "image_copyright_cleared": "Y"
      }"""
    )
    work.creators shouldBe List(Agent(secondaryCreator))
  }

  it("should use all the values in the image_creator_secondary field if image_creator is not present") {
    val secondaryCreator1 = "Gamekeeper Gordon"
    val secondaryCreator2 = "Herpetologist Harriet"
    val work = transformMiroRecord(
      data = s"""{
        "image_title": "Verdant and vivid",
        "image_secondary_creator": ["$secondaryCreator1", "$secondaryCreator2"],
        "image_cleared": "Y",
        "image_copyright_cleared": "Y"
      }"""
    )
    work.creators shouldBe List(Agent(secondaryCreator1), Agent(secondaryCreator2))
  }

  it("should combine the values in the image_creator and image_secondary_creator fields if both present") {
    val creator = "Mycologist Morgan"
    val secondaryCreator = "Manufacturer Mel"
    val work = transformMiroRecord(
      data = s"""{
        "image_title": "Musings on mice",
        "image_creator": ["$creator"],
        "image_secondary_creator": ["$secondaryCreator"],
        "image_cleared": "Y",
        "image_copyright_cleared": "Y"
      }"""
    )
    work.creators shouldBe List(Agent(creator), Agent(secondaryCreator))
  }

  it("should not pass through records with a missing image_cleared field") {
    assertTransformMiroRecordFails(data="""{
      "image_title": "Missives on museums",
      "image_copyright_cleared": "Y"
    }""")
  }

  it("should not pass through records with a missing image_copyright_cleared field") {
    assertTransformMiroRecordFails(data="""{
      "image_title": "A caricature of cats",
      "image_cleared": "Y"
    }""")
  }

  it("should not pass through records with missing image_cleared and missing image_copyright_cleared field") {
    assertTransformMiroRecordFails(data="""{
      "image_title": "Drawings of dromedaries"
    }""")
  }

  it("should not pass through records with an image_cleared value that isn't 'Y'") {
    assertTransformMiroRecordFails(data="""{
      "image_title": "Confidential colourings of crododiles",
      "image_cleared": "N",
      "image_copyright_cleared": "Y"
    }""")
  }

  it("should not pass through records with image_copyright_cleared field that isn't 'Y'") {
    assertTransformMiroRecordFails(data="""{
      "image_title": "Proprietary poetry about porcupines",
      "image_cleared": "Y",
      "image_copyright_cleared": "N"
    }""")
  }

  private def assertTransformMiroRecordFails(
    miroID: String = "M0000001",
    miroCollection: String = "TestCollection",
    data: String = """{"image_title": "A failed fumble in the fire"}"""
  ) = {
    val miroTransformable = MiroTransformable(
      MiroID = miroID,
      MiroCollection = miroCollection,
      data = data
    )

    miroTransformable.transform.isSuccess shouldBe false
  }

  private def transformMiroRecord(
    miroID: String = "M0000001",
    miroCollection: String = "TestCollection",
    data: String = """{
      "image_title": "A test tome telling tales about a tapir",
      "image_cleared": "Y",
      "image_copyright_cleared": "Y"
    }"""
  ): Work = {
    val miroTransformable = MiroTransformable(
      MiroID = miroID,
      MiroCollection = miroCollection,
      data = data
    )

    miroTransformable.transform.isSuccess shouldBe true
    miroTransformable.transform.get
  }
}
