package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models.{IdentifiedWork, Period}

class PublicationDateTest extends ApiWorksTestBase {

  it("omits the publicationDate field if it is empty") {
    val work = IdentifiedWork(
      canonicalId = "arfj5cj4",
      sourceIdentifier = sourceIdentifier,
      title = Some("Asking aging armadillos for another appraisal"),
      publicationDate = None
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
      publicationDate = Some(Period("1923"))
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

  it("includes the publishers field with a mixture of agents/organisations") {
    val work = IdentifiedWork(
      canonicalId = "v9w6cz66",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("Vultures vying for victory"),
      publishers = List(
        Agent("Vivian Violet"),
        Organisation("Verily Volumes")
      )
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
          |      "publishers": [
          |        {
          |          "label": "${work.publishers(0).label}",
          |          "type": "Agent"
          |        },
          |        {
          |          "label": "${work.publishers(1).label}",
          |          "type": "Organisation"
          |        }
          |      ]
          |    }
          |  ]
          |}
          """.stripMargin
      )
    }
  }
}
