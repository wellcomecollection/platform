package uk.ac.wellcome.platform.api.works.v1

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.models.work.internal._

class ApiV1WorksTest extends ApiV1WorksTestBase {

  it("returns a list of works") {
    withApiFixtures(ApiVersions.v1) {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val works = createWorks(3)

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
                 |     "description": "${works(0).description.get}",
                 |     "workType": {
                 |       "id": "${works(0).workType.get.id}",
                 |       "label": "${works(0).workType.get.label}",
                 |       "type": "WorkType"
                 |     },
                 |     "lettering": "${works(0).lettering.get}",
                 |     "createdDate": ${period(works(0).createdDate.get)},
                 |     "creators": [ ${identifiedOrUnidentifiable(
                                works(0).contributors(0).agent,
                                abstractAgent)} ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${works(1).canonicalId}",
                 |     "title": "${works(1).title}",
                 |     "description": "${works(1).description.get}",
                 |     "workType": {
                 |       "id": "${works(1).workType.get.id}",
                 |       "label": "${works(1).workType.get.label}",
                 |       "type": "WorkType"
                 |     },
                 |     "lettering": "${works(1).lettering.get}",
                 |     "createdDate": ${period(works(1).createdDate.get)},
                 |     "creators": [ ${identifiedOrUnidentifiable(
                                works(1).contributors(0).agent,
                                abstractAgent)} ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${works(2).canonicalId}",
                 |     "title": "${works(2).title}",
                 |     "description": "${works(2).description.get}",
                 |     "workType": {
                 |       "id": "${works(2).workType.get.id}",
                 |       "label": "${works(2).workType.get.label}",
                 |       "type": "WorkType"
                 |     },
                 |     "lettering": "${works(2).lettering.get}",
                 |     "createdDate": ${period(works(2).createdDate.get)},
                 |     "creators": [ ${identifiedOrUnidentifiable(
                                works(2).contributors(0).agent,
                                abstractAgent)} ],
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
    withApiFixtures(ApiVersions.v1) {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val work = createIdentifiedWorkWith(
          description = Some(s"A single work in ${this.getClass.getSimpleName}"),
          lettering = Some(s"Lettering on a work in ${this.getClass.getSimpleName}"),
          createdDate = Some(Period("The future")),
          contributors = List(Contributor(agent = Unidentifiable(agent))),
          items = createItems(count = 2)
        )

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
                 | "description": "${work.description.get}",
                 | "workType": {
                 |       "id": "${workType.id}",
                 |       "label": "${workType.label}",
                 |       "type": "WorkType"
                 | },
                 | "lettering": "${work.lettering.get}",
                 | "createdDate": ${period(work.createdDate.get)},
                 | "creators": [ ${identifiedOrUnidentifiable(
                                work.contributors(0).agent,
                                abstractAgent)} ],
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
    withApiFixtures(ApiVersions.v1) {
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
    withApiFixtures(ApiVersions.v1) {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val works = createWorks(3)

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
                 |     "description": "${works(1).description.get}",
                 |     "workType" : {
                 |        "id" : "${works(1).workType.get.id}",
                 |        "label" : "${works(1).workType.get.label}",
                 |        "type" : "WorkType"
                 |      },
                 |     "lettering": "${works(1).lettering.get}",
                 |     "createdDate": ${period(works(1).createdDate.get)},
                 |     "creators": [ ${identifiedOrUnidentifiable(
                                works(1).contributors(0).agent,
                                abstractAgent)} ],
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
                 |     "description": "${works(0).description.get}",
                 |     "workType" : {
                 |        "id" : "${works(0).workType.get.id}",
                 |        "label" : "${works(0).workType.get.label}",
                 |        "type" : "WorkType"
                 |      },
                 |     "lettering": "${works(0).lettering.get}",
                 |     "createdDate": ${period(works(0).createdDate.get)},
                 |     "creators": [ ${identifiedOrUnidentifiable(
                                works(0).contributors(0).agent,
                                abstractAgent)} ],
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
                 |     "description": "${works(2).description.get}",
                 |     "workType" : {
                 |        "id" : "${works(2).workType.get.id}",
                 |        "label" : "${works(2).workType.get.label}",
                 |        "type" : "WorkType"
                 |      },
                 |     "lettering": "${works(2).lettering.get}",
                 |     "createdDate": ${period(works(2).createdDate.get)},
                 |     "creators": [ ${identifiedOrUnidentifiable(
                                works(2).contributors(0).agent,
                                abstractAgent)} ],
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
    withApiFixtures(ApiVersions.v1) {
      case (apiPrefix, _, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?foo=bar",
          andExpect = Status.Ok,
          withJsonBody = emptyJsonResult(apiPrefix)
        )
    }
  }

  it("returns matching results if doing a full-text search") {
    withApiFixtures(ApiVersions.v1) {
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
    withApiFixtures(ApiVersions.v1) {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val identifier1 = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          ontologyType = "Work",
          value = "Test1234"
        )
        val work1 = createIdentifiedWorkWith(
          otherIdentifiers = List(identifier1)
        )

        val identifier2 = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          ontologyType = "Work",
          value = "DTest5678"
        )
        val work2 = createIdentifiedWorkWith(
          otherIdentifiers = List(identifier2)
        )

        insertIntoElasticsearch(indexNameV1, itemType, work1, work2)

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
                 |     "id": "${work1.canonicalId}",
                 |     "title": "${work1.title}",
                 |     "creators": [ ],
                 |     "identifiers": [ ${identifier(sourceIdentifier)}, ${identifier(
                                identifier1)} ],
                 |     "subjects": [ ],
                 |     "genres": [ ],
                 |     "publishers": [ ],
                 |     "placesOfPublication": [ ]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${work2.canonicalId}",
                 |     "title": "${work2.title}",
                 |     "creators": [ ],
                 |     "identifiers": [ ${identifier(sourceIdentifier)}, ${identifier(
                                identifier2)} ],
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
    withApiFixtures(ApiVersions.v1) {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        val srcIdentifier = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          ontologyType = "Work",
          value = "Test1234"
        )
        val work = createIdentifiedWorkWith(
          otherIdentifiers = List(srcIdentifier)
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
                 | "identifiers": [ ${identifier(sourceIdentifier)}, ${identifier(
                                srcIdentifier)} ],
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
    withApiFixtures(ApiVersions.v1) {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        withLocalElasticsearchIndex(itemType = itemType) { otherIndex =>
          val work = createIdentifiedWorkWith(title = "A whale on a wave")
          insertIntoElasticsearch(indexNameV1, itemType, work)

          val work_alt = createIdentifiedWorkWith(title = "An impostor in an igloo")
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
    withApiFixtures(ApiVersions.v1) {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        withLocalElasticsearchIndex(itemType = itemType) { otherIndex =>
          val work = createIdentifiedWorkWith(
            title = "A wombat wallowing under a willow"
          )
          insertIntoElasticsearch(indexNameV1, itemType, work)

          val work_alt = createIdentifiedWorkWith(
            title = "An impostor in an igloo"
          )
          insertIntoElasticsearch(
            indexName = otherIndex,
            itemType = itemType,
            work_alt)

          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works?query=wombat",
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
              path = s"/$apiPrefix/works?query=igloo&_index=$otherIndex",
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
    withApiFixtures(ApiVersions.v1) {
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
    withApiFixtures(ApiVersions.v1) {
      case (
          apiPrefix,
          indexNameV1,
          indexNameV2,
          itemType,
          server: EmbeddedHttpServer) =>
        val work1 = createIdentifiedWorkWith(
          title = "A wombat wallowing under a willow"
        )

        insertIntoElasticsearch(indexNameV1, itemType, work1)

        val work2 = createIdentifiedWorkWith(
          title = "A wombat wrestling with wet weather"
        )

        insertIntoElasticsearch(indexNameV2, itemType, work2)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?query=wombat",
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
