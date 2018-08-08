package uk.ac.wellcome.platform.api.works.v2

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.models.work.internal.{Concept, Subject, Unidentifiable}

class ApiV2WorksIncludesTest extends ApiV2WorksTestBase {
  it(
    "includes a list of identifiers on a list endpoint if we pass ?include=identifiers") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 2).sortBy { _.canonicalId }

        val identifier0 = createSourceIdentifier
        val identifier1 = createSourceIdentifier

        val work0 = works(0).copy(otherIdentifiers = List(identifier0))
        val work1 = works(1).copy(otherIdentifiers = List(identifier1))

        insertIntoElasticsearch(indexNameV2, itemType, work0, work1)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?include=identifiers",
            andExpect = Status.Ok,
            withJsonBody =
              s"""
                              |{
                              |  ${resultList(apiPrefix, totalResults = 2)},
                              |  "results": [
                              |   {
                              |     "type": "Work",
                              |     "id": "${work0.canonicalId}",
                              |     "title": "${work0.title}",
                              |     "contributors": [ ],
                              |     "identifiers": [ ${identifier(
                   work0.sourceIdentifier)}, ${identifier(identifier0)} ],
                              |     "genres": [ ],
                              |     "production": [ ]
                              |   },
                              |   {
                              |     "type": "Work",
                              |     "id": "${work1.canonicalId}",
                              |     "title": "${work1.title}",
                              |     "contributors": [ ],
                              |     "identifiers": [ ${identifier(
                   work1.sourceIdentifier)}, ${identifier(identifier1)} ],
                              |     "genres": [ ],
                              |     "production": [ ]
                              |   }
                              |  ]
                              |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of identifiers on a single work endpoint if we pass ?include=identifiers") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val otherIdentifier = createSourceIdentifier
        val work = createIdentifiedWorkWith(
          otherIdentifiers = List(otherIdentifier)
        )
        insertIntoElasticsearch(indexNameV2, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?include=identifiers",
            andExpect = Status.Ok,
            withJsonBody =
              s"""
                              |{
                              | "@context": "https://localhost:8888/$apiPrefix/context.json",
                              | "type": "Work",
                              | "id": "${work.canonicalId}",
                              | "title": "${work.title}",
                              | "contributors": [ ],
                              | "identifiers": [ ${identifier(
                   work.sourceIdentifier)}, ${identifier(otherIdentifier)} ],
                              | "genres": [ ],
                              | "production": [ ]
                              |}
          """.stripMargin
          )
        }
    }
  }

  it("renders the items if the items include is present") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val work = createIdentifiedWorkWith(
          items = createIdentifiedItems(count = 1) :+ createUnidentifiableItemWith()
        )

        insertIntoElasticsearch(indexNameV2, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?include=items",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              | "@context": "https://localhost:8888/$apiPrefix/context.json",
                              | "type": "Work",
                              | "id": "${work.canonicalId}",
                              | "title": "${work.title}",
                              | "contributors": [ ],
                              | "items": [ ${items(work.items)} ],
                              | "genres": [ ],
                              | "production": [ ]
                              |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of subjects on a list endpoint if we pass ?include=subjects") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 2).sortBy { _.canonicalId }


        val subjects1 = List(Subject("ornitology", List(Unidentifiable(Concept("ornitology")))))
        val subjects2 = List(Subject("flying cars", List(Unidentifiable(Concept("flying cars")))))
        val work0 = works(0).copy(subjects = subjects1)
        val work1 = works(1).copy(subjects = subjects2)

        insertIntoElasticsearch(indexNameV2, itemType, work0, work1)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?include=subjects",
            andExpect = Status.Ok,
            withJsonBody =
              s"""
                 |{
                 |  ${resultList(apiPrefix, totalResults = 2)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${work0.canonicalId}",
                 |     "title": "${work0.title}",
                 |     "contributors": [ ],
                 |     "subjects": [ ${subjects(subjects1)}],
                 |     "genres": [ ],
                 |     "production": [ ]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${work1.canonicalId}",
                 |     "title": "${work1.title}",
                 |     "contributors": [ ],
                 |     "subjects": [ ${subjects(subjects2)}],
                 |     "genres": [ ],
                 |     "production": [ ]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )
        }
    }
  }

}
