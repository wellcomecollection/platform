package uk.ac.wellcome.platform.api.works.v1

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.models.work.internal._

class ApiV1WorksTest extends ApiV1WorksTestBase {

  it("returns a list of works") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 3).sortBy { _.canonicalId }

        insertIntoElasticsearch(indexNameV1, itemType, works: _*)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(apiPrefix, totalResults = 3)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${works(0).canonicalId}",
                 |     "title": "${works(0).title}",
                 |     "creators": [ ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${works(1).canonicalId}",
                 |     "title": "${works(1).title}",
                 |     "creators": [ ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${works(2).canonicalId}",
                 |     "title": "${works(2).title}",
                 |     "creators": [ ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it("returns a single work when requested with id") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val work = createIdentifiedWork

        insertIntoElasticsearch(indexNameV1, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 | "@context": "https://localhost:8888/$apiPrefix/context.json",
                 | "type": "Work",
                 | "id": "${work.canonicalId}",
                 | "title": "${work.title}",
                 | "creators": [ ],
                 | "subjects": [ ],
                 | "genres": [ ],
                 | "publishers": [ ],
                 | "placesOfPublication": [ ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it("renders the items if the items include is present") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val work = createIdentifiedWorkWith(
          items = createItems(count = 1)
        )

        insertIntoElasticsearch(indexNameV1, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?includes=items",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 | "@context": "https://localhost:8888/$apiPrefix/context.json",
                 | "type": "Work",
                 | "id": "${work.canonicalId}",
                 | "title": "${work.title}",
                 | "creators": [ ],
                 | "items": [ ${items(work.items)} ],
                 | "subjects": [ ],
                 | "genres": [ ],
                 | "publishers": [ ],
                 | "placesOfPublication": [ ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "returns the requested page of results when requested with page & pageSize") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 3).sortBy { _.canonicalId }

        insertIntoElasticsearch(indexNameV1, itemType, works: _*)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?page=2&pageSize=1",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(
                                apiPrefix,
                                pageSize = 1,
                                totalPages = 3,
                                totalResults = 3)},
                 |  "prevPage": "https://localhost:8888/$apiPrefix/works?page=1&pageSize=1",
                 |  "nextPage": "https://localhost:8888/$apiPrefix/works?page=3&pageSize=1",
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${works(1).canonicalId}",
                 |     "title": "${works(1).title}",
                 |     "creators": [ ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   }]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )

          server.httpGet(
            path = s"/$apiPrefix/works?page=1&pageSize=1",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(
                                apiPrefix,
                                pageSize = 1,
                                totalPages = 3,
                                totalResults = 3)},
                 |  "nextPage": "https://localhost:8888/$apiPrefix/works?page=2&pageSize=1",
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${works(0).canonicalId}",
                 |     "title": "${works(0).title}",
                 |     "creators": [ ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   }]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )

          server.httpGet(
            path = s"/$apiPrefix/works?page=3&pageSize=1",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(
                                apiPrefix,
                                pageSize = 1,
                                totalPages = 3,
                                totalResults = 3)},
                 |  "prevPage": "https://localhost:8888/$apiPrefix/works?page=2&pageSize=1",
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${works(2).canonicalId}",
                 |     "title": "${works(2).title}",
                 |     "creators": [ ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   }]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it("ignores parameters that are unused when making an API request") {
    withV1Api {
      case (apiPrefix, _, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?foo=bar",
          andExpect = Status.Ok,
          withJsonBody = emptyJsonResult(apiPrefix)
        )
    }
  }

  it("returns matching results if doing a full-text search") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val work1 = createIdentifiedWorkWith(
          title = "A drawing of a dodo"
        )
        val work2 = createIdentifiedWorkWith(
          title = "A mezzotint of a mouse"
        )
        insertIntoElasticsearch(indexNameV1, itemType, work1, work2)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?query=cat",
            andExpect = Status.Ok,
            withJsonBody = emptyJsonResult(apiPrefix)
          )
        }

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?query=dodo",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(apiPrefix)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${work1.canonicalId}",
                 |     "title": "${work1.title}",
                 |     "creators": [],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   }
                 |  ]
                 |}""".stripMargin
          )
        }
    }
  }

  it(
    "includes a list of identifiers on a list endpoint if we pass ?includes=identifiers") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 2).sortBy { _.canonicalId }

        val identifier0 = createSourceIdentifier
        val identifier1 = createSourceIdentifier

        val work0 = works(0).copy(otherIdentifiers = List(identifier0))
        val work1 = works(1).copy(otherIdentifiers = List(identifier1))

        insertIntoElasticsearch(indexNameV1, itemType, work0, work1)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?includes=identifiers",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(apiPrefix, totalResults = 2)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${work0.canonicalId}",
                 |     "title": "${work0.title}",
                 |     "creators": [ ],
                 |     "identifiers": [ ${identifier(work0.sourceIdentifier)}, ${identifier(
                                identifier0)} ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${work1.canonicalId}",
                 |     "title": "${work1.title}",
                 |     "creators": [ ],
                 |     "identifiers": [ ${identifier(work1.sourceIdentifier)}, ${identifier(
                                identifier1)} ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of identifiers on a single work endpoint if we pass ?includes=identifiers") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val otherIdentifier = createSourceIdentifier
        val work = createIdentifiedWorkWith(
          otherIdentifiers = List(otherIdentifier)
        )
        insertIntoElasticsearch(indexNameV1, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?includes=identifiers",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 | "@context": "https://localhost:8888/$apiPrefix/context.json",
                 | "type": "Work",
                 | "id": "${work.canonicalId}",
                 | "title": "${work.title}",
                 | "creators": [ ],
                 | "identifiers": [ ${identifier(work.sourceIdentifier)}, ${identifier(
                                otherIdentifier)} ],
                 | "subjects": [ ],
                 | "genres": [ ],
                 | "publishers": [ ],
                 | "placesOfPublication": [ ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it("searches different indices with the ?_index query parameter") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        withLocalElasticsearchIndex(itemType = itemType) { otherIndex =>
          val work = createIdentifiedWork
          insertIntoElasticsearch(indexNameV1, itemType, work)

          val work_alt = createIdentifiedWork
          insertIntoElasticsearch(
            indexName = otherIndex,
            itemType = itemType,
            work_alt)

          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works/${work.canonicalId}",
              andExpect = Status.Ok,
              withJsonBody = s"""
                   |{
                   | "@context": "https://localhost:8888/$apiPrefix/context.json",
                   | "type": "Work",
                   | "id": "${work.canonicalId}",
                   | "title": "${work.title}",
                   | "creators": [ ],
                   | "subjects": [ ],
                   | "genres": [ ],
                   | "publishers": [ ],
                   | "placesOfPublication": [ ]
                   |}
          """.stripMargin
            )
          }

          eventually {
            server.httpGet(
              path =
                s"/$apiPrefix/works/${work_alt.canonicalId}?_index=$otherIndex",
              andExpect = Status.Ok,
              withJsonBody = s"""
                   |{
                   | "@context": "https://localhost:8888/$apiPrefix/context.json",
                   | "type": "Work",
                   | "id": "${work_alt.canonicalId}",
                   | "title": "${work_alt.title}",
                   | "creators": [ ],
                   | "subjects": [ ],
                   | "genres": [ ],
                   | "publishers": [ ],
                   | "placesOfPublication": [ ]
                   |}
          """.stripMargin
            )
          }
        }
    }
  }

  it("looks up works in different indices with the ?_index query parameter") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        withLocalElasticsearchIndex(itemType = itemType) { otherIndex =>
          val work = createIdentifiedWorkWith(
            title = "Wombles of Wimbledon"
          )
          insertIntoElasticsearch(indexNameV1, itemType, work)

          val work_alt = createIdentifiedWorkWith(
            title = work.title
          )
          insertIntoElasticsearch(
            indexName = otherIndex,
            itemType = itemType,
            work_alt)

          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works?query=wombles",
              andExpect = Status.Ok,
              withJsonBody = s"""
                   |{
                   |  ${resultList(apiPrefix)},
                   |  "results": [
                   |   {
                   |     "type": "Work",
                   |     "id": "${work.canonicalId}",
                   |     "title": "${work.title}",
                   |     "creators": [ ],
                   |     "subjects": [ ],
                   |     "genres": [ ],
                   |     "publishers": [ ],
                   |     "placesOfPublication": [ ]
                   |   }
                   |  ]
                   |}
          """.stripMargin
            )
          }

          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works?query=wombles&_index=$otherIndex",
              andExpect = Status.Ok,
              withJsonBody = s"""
                   |{
                   |  ${resultList(apiPrefix)},
                   |  "results": [
                   |   {
                   |     "type": "Work",
                   |     "id": "${work_alt.canonicalId}",
                   |     "title": "${work_alt.title}",
                   |     "creators": [ ],
                   |     "subjects": [ ],
                   |     "genres": [ ],
                   |     "publishers": [ ],
                   |     "placesOfPublication": [ ]
                   |   }
                   |  ]
                   |}
          """.stripMargin
            )
          }
        }
    }
  }

  it(
    "includes the thumbnail field if available and we use the thumbnail include") {
    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val work = createIdentifiedWorkWith(
          thumbnail = Some(
            DigitalLocation(
              locationType = LocationType("thumbnail-image"),
              url = "https://iiif.example.org/1234/default.jpg",
              license = License_CCBY
            ))
        )
        insertIntoElasticsearch(indexNameV1, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?includes=thumbnail",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(apiPrefix)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${work.canonicalId}",
                 |     "title": "${work.title}",
                 |     "creators": [ ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ],
                 |     "thumbnail": ${location(work.thumbnail.get)}
                 |    }
                 |  ]
                 |}
                 |
            """.stripMargin
          )
        }
    }
  }

  it("only returns works from the v1 index") {
    withV1Api {
      case (
          apiPrefix,
          indexNameV1,
          indexNameV2,
          itemType,
          server: EmbeddedHttpServer) =>
        val work1 = createIdentifiedWorkWith(
          title = "One orange ocelot"
        )
        insertIntoElasticsearch(indexNameV1, itemType, work1)

        val work2 = createIdentifiedWorkWith(title = work1.title)
        insertIntoElasticsearch(indexNameV2, itemType, work2)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?query=ocelot",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(apiPrefix)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${work1.canonicalId}",
                 |     "title": "${work1.title}",
                 |     "creators": [ ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )
        }
    }
  }
}
