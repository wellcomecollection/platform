package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models.{Agent, Organisation, Work}

class PublishersTest extends ApiWorksTestBase {

  it("includes an empty publishers field if the work has no publishers") {
    val work = Work(
      canonicalId = Some("zm9q6c6h"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("A zoo of zebras doing zumba"),
      publishers = List()
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
          |      "id": "${work.id}",
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

  it("includes the publishers field for agent publishers") {
    val work = Work(
      canonicalId = Some("patkj4ds"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("A party of purple panthers in Paris"),
      publishers = List(
        Agent("Percy Parrot"),
        Agent("Patricia Parrakeet")
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
          |      "id": "${work.id}",
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
          |          "type": "Agent"
          |        }
          |      ]
          |    }
          |  ]
          |}
          """.stripMargin
      )
    }
  }

  it("includes the publishers field with a mixture of agents/organisations") {
    val work = Work(
      canonicalId = Some("v9w6cz66"),
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
          |      "id": "${work.id}",
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
