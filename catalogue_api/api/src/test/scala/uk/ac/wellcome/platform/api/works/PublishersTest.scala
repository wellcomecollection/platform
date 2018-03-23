package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models.{
  Agent,
  IdentifiedWork,
  Organisation,
  Person,
  Unidentifiable
}

class PublishersTest extends ApiWorksTestBase {

  it("includes an empty publishers field if the work has no publishers") {
    val work = IdentifiedWork(
      canonicalId = "zm9q6c6h",
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

  it("includes the publishers field for agent publishers") {
    val work = IdentifiedWork(
      canonicalId = "patkj4ds",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("A party of purple panthers in Paris"),
      publishers = List(
        Unidentifiable(Agent("Percy Parrot")),
        Unidentifiable(Agent("Patricia Parrakeet"))
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
          |        ${identifiedOrUnidentifiable(
                            work.publishers(0),
                            abstractAgent)},
          |        ${identifiedOrUnidentifiable(
                            work.publishers(1),
                            abstractAgent)}
          |      ],
          |      "placesOfPublication": [ ]
          |    }
          |  ]
          |}
          """.stripMargin
      )
    }
  }

  it(
    "includes the publishers field with a mixture of agents/organisations/persons") {

    val work = IdentifiedWork(
      canonicalId = "v9w6cz66",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("Vultures vying for victory"),
      publishers = List(
        Unidentifiable(Agent("Vivian Violet")),
        Unidentifiable(Organisation("Verily Volumes")),
        Unidentifiable(
          Person(
            label = "Havelock Vetinari",
            prefix = Some("Lord Patrician"),
            numeration = Some("I")))
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
          |        ${identifiedOrUnidentifiable(
                            work.publishers(0),
                            abstractAgent)},
          |        ${identifiedOrUnidentifiable(
                            work.publishers(1),
                            abstractAgent)},
          |        ${identifiedOrUnidentifiable(
                            work.publishers(2),
                            abstractAgent)}
          |      ],
          |      "placesOfPublication": [ ]
          |    }
          |  ]
          |}
          """.stripMargin
      )
    }
  }
}
