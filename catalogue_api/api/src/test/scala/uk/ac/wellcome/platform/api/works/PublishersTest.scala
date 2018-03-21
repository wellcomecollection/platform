package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models.{Agent, IdentifiedWork, Organisation, Person}

class PublishersTest extends ApiWorksTestBase {

  it("includes an empty publishers field if the work has no publishers") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work = IdentifiedWork(
          canonicalId = "zm9q6c6h",
          sourceIdentifier = sourceIdentifier,
          version = 1,
          title = Some("A zoo of zebras doing zumba"),
          publishers = List()
        )

        insertIntoElasticsearch(indexName, itemType, work)

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
    }
  }

  it("includes the publishers field for agent publishers") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work = IdentifiedWork(
          canonicalId = "patkj4ds",
          sourceIdentifier = sourceIdentifier,
          version = 1,
          title = Some("A party of purple panthers in Paris"),
          publishers = List(
            Agent("Percy Parrot"),
            Agent("Patricia Parrakeet")
          )
        )

        insertIntoElasticsearch(indexName, itemType, work)

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
              |          "type": "Agent"
              |        }
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
  }

  it("includes the publishers field with a mixture of agents/organisations/persons") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work = IdentifiedWork(
          canonicalId = "v9w6cz66",
          sourceIdentifier = sourceIdentifier,
          version = 1,
          title = Some("Vultures vying for victory"),
          publishers = List(
            Agent("Vivian Violet"),
            Organisation("Verily Volumes"),
            Person(
              label = "Havelock Vetinari",
              prefix = Some("Lord Patrician"),
              numeration = Some("I")
            )
          )
        )

        insertIntoElasticsearch(indexName, itemType, work)

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
              |        ${abstractAgent(work.publishers(0))},
              |        ${abstractAgent(work.publishers(1))},
              |        ${abstractAgent(work.publishers(2))}
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
  }
}
