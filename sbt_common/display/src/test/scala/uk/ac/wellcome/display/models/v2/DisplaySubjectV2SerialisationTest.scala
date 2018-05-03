package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal.{Concept, Period, Subject}

class DisplaySubjectV2SerialisationTest
    extends FunSpec
    with JsonMapperTestUtil {

  it("serialises a DisplaySubject constructed from a Subject correctly") {
    assertObjectMapsToJson(
      DisplaySubject(
        Subject(
          "subjectLabel",
          List(Concept("conceptLabel"), Period("periodLabel")))),
      expectedJson = s"""
         |  {
         |    "label" : "subjectLabel",
         |    "concepts" : [
         |      {
         |        "label" : "conceptLabel",
         |        "type" : "Concept"
         |      },
         |      {
         |        "label" : "periodLabel",
         |        "type" : "Period"
         |      }
         |    ],
         |    "type" : "Subject"
         |  }
          """.stripMargin
    )
  }
}
