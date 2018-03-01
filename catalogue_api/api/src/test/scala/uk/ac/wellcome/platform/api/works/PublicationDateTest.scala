package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models.{IdentifiedWork, Period}

class PublicationDateTest extends ApiWorksTestBase {

  it("omits the publicationDate field if it is empty") {
    val work = IdentifiedWork(
      canonicalId = "arfj5cj4",
      sourceIdentifier = sourceIdentifier,
      title = Some("Asking aging armadillos for another appraisal"),
      publicationDate = None,
      version = 1
    )

    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works",
        andExpect = Status.Ok,
        withJsonBody = s"""
          |{
          |  ${resultList(totalResults = 1)},
          |  "results": [
          |    {
          |      "type": "Work",
          |      "id": "${work.canonicalId}",
          |      "title": "${work.title.get}",
          |      "creators": [ ],
          |      "subjects": [ ],
          |      "genres": [ ],
          |      "publishers": [ ]
          |    }
          |  ]
          |}
          """.stripMargin
      )
    }
  }

  it("includes the publicationDate field if it is present on the Work") {
    val work = IdentifiedWork(
      canonicalId = "avfpwgrr",
      sourceIdentifier = sourceIdentifier,
      title = Some("Ahoy!  Armoured angelfish are attacking the armada!"),
      publicationDate = Some(Period("1923")),
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
          |       }
          |     }
          |   ]
          |}
          """.stripMargin
      )
    }
  }
}
