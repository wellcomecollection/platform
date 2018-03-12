package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models.{IdentifiedWork, Period, Place}

class PlaceOfPublicationTest extends ApiWorksTestBase {

  it("includes the placesOfPublication field") {
    val work = IdentifiedWork(
      canonicalId = "avfpwgrr",
      sourceIdentifier = sourceIdentifier,
      title = Some("Ahoy!  Armoured angelfish are attacking the armada!"),
      publicationDate = Some(Period("1923")),
      placesOfPublication = List(Place("Durmstrang")),
      version = 1
    )

    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works",
        andExpect = Status.Ok,
        withJsonBody = s"""
          |{
          | ${resultList(totalResults = 1)},
          |   "results": [
          |     {
          |       "type": "Work",
          |       "id": "${work.canonicalId}",
          |       "title": "${work.title.get}",
          |       "creators": [ ],
          |       "subjects": [ ],
          |       "genres": [ ],
          |       "publishers": [ ],
          |       "publicationDate": {
          |         "label": "${work.publicationDate.get.label}",
          |         "type": "Period"
          |       },
          |       "placesOfPublication": [
          |         {
          |           "label": "${work.placesOfPublication.head.label}",
          |           "type": "Place"
          |         }
          |       ]
          |     }
          |   ]
          |}
          """.stripMargin
      )
    }
  }
}
