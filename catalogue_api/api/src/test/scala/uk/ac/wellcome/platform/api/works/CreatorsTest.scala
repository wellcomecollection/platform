package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models._

class CreatorsTest extends ApiWorksTestBase {

  it("includes an empty creators field if the work has no creators") {
    val work = IdentifiedWork(
      canonicalId = "zm9q6c6h",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("A zoo of zebras doing zumba"),
      creators = List()
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
          |      "publishers": [ ],
          |      "placesOfPublication": [ ]
          |    }
          |  ]
          |}
          """.stripMargin
      )
    }
  }

  it(
    "includes the creators field with a mixture of agents/organisations/persons") {
    val work = IdentifiedWork(
      canonicalId = "v9w6cz66",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("Vultures vying for victory"),
      creators = List(
        Unidentifiable(Agent("Vivian Violet")),
        Unidentifiable(Organisation("Verily Volumes")),
        Unidentifiable(Person(label = "Havelock Vetinari", prefix = Some("Lord Patrician"), numeration = Some("I")))
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
          |      "creators": [
          |        ${identifiedOrUnidentifiable(work.creators(0), abstractAgent)},
          |        ${identifiedOrUnidentifiable(work.creators(0), abstractAgent)},
          |        ${identifiedOrUnidentifiable(work.creators(0), abstractAgent)}
          |      ],
          |      "subjects": [ ],
          |      "genres": [ ],
          |      "publishers": [],
          |      "placesOfPublication": [ ]
          |    }
          |  ]
          |}
          """.stripMargin
      )
    }
  }
}
