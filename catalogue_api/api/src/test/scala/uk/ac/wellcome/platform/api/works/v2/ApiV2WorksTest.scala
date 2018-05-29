package uk.ac.wellcome.platform.api.works.v2

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.models.work.internal.{
  DigitalLocation,
  IdentifierType,
  License_CCBY,
  SourceIdentifier
}
import uk.ac.wellcome.platform.api.works.ApiWorksTestBase

class ApiV2WorksTest extends ApiWorksTestBase {
  def withV2Api[R] = withApiFixtures[R](ApiVersions.v2)(_)

  it("returns a list of works") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val works = createWorks(3)

        insertIntoElasticsearch(indexNameV2, itemType, works: _*)

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
               |     "title": "${works(0).title.get}",
               |     "description": "${works(0).description.get}",
               |     "workType": {
               |       "id": "${works(0).workType.get.id}",
               |       "label": "${works(0).workType.get.label}",
               |       "type": "WorkType"
               |     },
               |     "lettering": "${works(0).lettering.get}",
               |     "createdDate": ${period(works(0).createdDate.get)},
               |     "contributors": [${contributor(works(0).contributors(0))}],
               |     "subjects": [ ],
               |     "genres": [ ],
               |     "publishers": [ ],
               |     "placesOfPublication": [ ]
               |   },
               |   {
               |     "type": "Work",
               |     "id": "${works(1).canonicalId}",
               |     "title": "${works(1).title.get}",
               |     "description": "${works(1).description.get}",
               |     "workType": {
               |       "id": "${works(1).workType.get.id}",
               |       "label": "${works(1).workType.get.label}",
               |       "type": "WorkType"
               |     },
               |     "lettering": "${works(1).lettering.get}",
               |     "createdDate": ${period(works(1).createdDate.get)},
               |     "contributors": [${contributor(works(1).contributors(0))}],
               |     "subjects": [ ],
               |     "genres": [ ],
               |     "publishers": [ ],
               |     "placesOfPublication": [ ]
               |   },
               |   {
               |     "type": "Work",
               |     "id": "${works(2).canonicalId}",
               |     "title": "${works(2).title.get}",
               |     "description": "${works(2).description.get}",
               |     "workType": {
               |       "id": "${works(2).workType.get.id}",
               |       "label": "${works(2).workType.get.label}",
               |       "type": "WorkType"
               |     },
               |     "lettering": "${works(2).lettering.get}",
               |     "createdDate": ${period(works(2).createdDate.get)},
               |     "contributors": [${contributor(works(2).contributors(0))}],
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
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val work = workWith(
          canonicalId = canonicalId,
          title = title,
          description = description,
          lettering = lettering,
          createdDate = period,
          creator = agent,
          subjects = List(subject),
          genres = List(genre),
          items = List(defaultItem),
          visible = true
        )

        insertIntoElasticsearch(indexNameV2, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/$canonicalId",
            andExpect = Status.Ok,
            withJsonBody = s"""
               |{
               | "@context": "https://localhost:8888/$apiPrefix/context.json",
               | "type": "Work",
               | "id": "$canonicalId",
               | "title": "$title",
               | "description": "$description",
               | "workType": {
               |       "id": "${workType.id}",
               |       "label": "${workType.label}",
               |       "type": "WorkType"
               | },
               | "lettering": "$lettering",
               | "createdDate": ${period(work.createdDate.get)},
               | "contributors": [${contributor(work.contributors(0))}],
               | "subjects": [
               |   { "label": "${subject.label}",
               |     "type": "${subject.ontologyType}",
               |     "concepts":[
               |       {
               |         "label": "${subject.concepts(0).agent.label}",
               |         "type":  "${subject.concepts(0).agent.ontologyType}"
               |       },
               |       {
               |         "label": "${subject.concepts(1).agent.label}",
               |         "type":  "${subject.concepts(1).agent.ontologyType}"
               |       },
               |       {
               |         "label": "${subject.concepts(2).agent.label}",
               |         "type":  "${subject.concepts(2).agent.ontologyType}"
               |       }]}
               | ],
               | "genres": [
               |   { "label": "${genre.label}",
               |     "type": "${genre.ontologyType}",
               |     "concepts":[
               |       {
               |         "label": "${genre.concepts(0).agent.label}",
               |         "type":  "${genre.concepts(0).agent.ontologyType}"
               |       },
               |       {
               |         "label": "${genre.concepts(1).agent.label}",
               |         "type":  "${genre.concepts(1).agent.ontologyType}"
               |       },
               |       {
               |         "label": "${genre.concepts(2).agent.label}",
               |         "type":  "${genre.concepts(2).agent.ontologyType}"
               |       }]}
               | ],
               | "publishers": [ ],
               | "placesOfPublication": [ ]
               |}
          """.stripMargin
          )
        }
    }
  }

  it("renders the items if the items include is present") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val work = workWith(
          canonicalId = "b4heraz7",
          title = "Inside an irate igloo",
          items = List(
            itemWith(
              canonicalId = "c3a599u5",
              identifier = defaultItemSourceIdentifier,
              location = defaultLocation
            )
          )
        )

        insertIntoElasticsearch(indexNameV2, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?includes=items",
            andExpect = Status.Ok,
            withJsonBody = s"""
               |{
               | "@context": "https://localhost:8888/$apiPrefix/context.json",
               | "type": "Work",
               | "id": "${work.canonicalId}",
               | "title": "${work.title.get}",
               | "contributors": [ ],
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
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val works = createWorks(3)

        insertIntoElasticsearch(indexNameV2, itemType, works: _*)

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
               |     "title": "${works(1).title.get}",
               |     "description": "${works(1).description.get}",
               |     "workType" : {
               |        "id" : "${works(1).workType.get.id}",
               |        "label" : "${works(1).workType.get.label}",
               |        "type" : "WorkType"
               |      },
               |     "lettering": "${works(1).lettering.get}",
               |     "createdDate": ${period(works(1).createdDate.get)},
               |     "contributors": [${contributor(works(1).contributors(0))}],
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
               |     "title": "${works(0).title.get}",
               |     "description": "${works(0).description.get}",
               |     "workType" : {
               |        "id" : "${works(0).workType.get.id}",
               |        "label" : "${works(0).workType.get.label}",
               |        "type" : "WorkType"
               |      },
               |     "lettering": "${works(0).lettering.get}",
               |     "createdDate": ${period(works(0).createdDate.get)},
               |     "contributors": [${contributor(works(0).contributors(0))}],
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
               |     "title": "${works(2).title.get}",
               |     "description": "${works(2).description.get}",
               |     "workType" : {
               |        "id" : "${works(2).workType.get.id}",
               |        "label" : "${works(2).workType.get.label}",
               |        "type" : "WorkType"
               |      },
               |     "lettering": "${works(2).lettering.get}",
               |     "createdDate": ${period(works(2).createdDate.get)},
               |     "contributors": [${contributor(works(2).contributors(0))}],
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
    withV2Api {
      case (apiPrefix, _, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?foo=bar",
          andExpect = Status.Ok,
          withJsonBody = emptyJsonResult(apiPrefix)
        )
    }
  }

  it("returns matching results if doing a full-text search") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val work1 = workWith(
          canonicalId = "1234",
          title = "A drawing of a dodo"
        )
        val work2 = workWith(
          canonicalId = "5678",
          title = "A mezzotint of a mouse"
        )
        insertIntoElasticsearch(indexNameV2, itemType, work1, work2)

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
               |     "title": "${work1.title.get}",
               |     "contributors": [],
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
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val identifier1 = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          ontologyType = "Work",
          value = "Test1234"
        )
        val work1 = workWith(
          canonicalId = "1234",
          title = "An image of an iguana",
          identifiers = List(identifier1)
        )

        val identifier2 = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          ontologyType = "Work",
          value = "DTest5678"
        )
        val work2 = workWith(
          canonicalId = "5678",
          title = "An impression of an igloo",
          identifiers = List(identifier2)
        )

        insertIntoElasticsearch(indexNameV2, itemType, work1, work2)

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
               |     "title": "${work1.title.get}",
               |     "contributors": [ ],
               |     "identifiers": [ ${identifier(identifier1)} ],
               |     "subjects": [ ],
               |     "genres": [ ],
               |     "publishers": [ ],
               |     "placesOfPublication": [ ]
               |   },
               |   {
               |     "type": "Work",
               |     "id": "${work2.canonicalId}",
               |     "title": "${work2.title.get}",
               |     "contributors": [ ],
               |     "identifiers": [ ${identifier(identifier2)} ],
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
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val srcIdentifier = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          ontologyType = "Work",
          value = "Test1234"
        )
        val work = workWith(
          canonicalId = "1234",
          title = "An insect huddled in an igloo",
          identifiers = List(srcIdentifier)
        )
        insertIntoElasticsearch(indexNameV2, itemType, work)

        eventually {
          server.httpGet(
            path =
              s"/$apiPrefix/works/${work.canonicalId}?includes=identifiers",
            andExpect = Status.Ok,
            withJsonBody = s"""
               |{
               | "@context": "https://localhost:8888/$apiPrefix/context.json",
               | "type": "Work",
               | "id": "${work.canonicalId}",
               | "title": "${work.title.get}",
               | "contributors": [ ],
               | "identifiers": [ ${identifier(srcIdentifier)} ],
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
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val work = workWith(
          canonicalId = "1234",
          title = "A whale on a wave"
        )
        insertIntoElasticsearch(indexNameV2, itemType, work)

        val work_alt = workWith(
          canonicalId = "5678",
          title = "An impostor in an igloo"
        )
        insertIntoElasticsearch(
          indexName = "alt_records",
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
               | "title": "${work.title.get}",
               | "contributors": [ ],
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
              s"/$apiPrefix/works/${work_alt.canonicalId}?_index=alt_records",
            andExpect = Status.Ok,
            withJsonBody = s"""
               |{
               | "@context": "https://localhost:8888/$apiPrefix/context.json",
               | "type": "Work",
               | "id": "${work_alt.canonicalId}",
               | "title": "${work_alt.title.get}",
               | "contributors": [ ],
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

  it("looks up works in different indices with the ?_index query parameter") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val work = workWith(
          canonicalId = "1234",
          title = "A wombat wallowing under a willow"
        )
        insertIntoElasticsearch(indexNameV2, itemType, work)

        val work_alt = workWith(
          canonicalId = "5678",
          title = "An impostor in an igloo"
        )
        insertIntoElasticsearch(
          indexName = "alt_records",
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
               |     "title": "${work.title.get}",
               |     "contributors": [ ],
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
            path = s"/$apiPrefix/works?query=igloo&_index=alt_records",
            andExpect = Status.Ok,
            withJsonBody = s"""
               |{
               |  ${resultList(apiPrefix)},
               |  "results": [
               |   {
               |     "type": "Work",
               |     "id": "${work_alt.canonicalId}",
               |     "title": "${work_alt.title.get}",
               |     "contributors": [ ],
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
    "includes the thumbnail field if available and we use the thumbnail include") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val work = identifiedWorkWith(
          canonicalId = "1234",
          title = "A thorn in the thumb tells a traumatic tale",
          thumbnail = DigitalLocation(
            locationType = "thumbnail-image",
            url = "https://iiif.example.org/1234/default.jpg",
            license = License_CCBY
          )
        )
        insertIntoElasticsearch(indexNameV2, itemType, work)

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
               |     "title": "${work.title.get}",
               |     "contributors": [ ],
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

  it("only returns works from the v2 index") {
    withV2Api {
      case (
          apiPrefix,
          indexNameV1,
          indexNameV2,
          itemType,
          server: EmbeddedHttpServer) =>
        val work1 = workWith(
          canonicalId = "1234",
          title = "A wombat wallowing under a willow"
        )

        insertIntoElasticsearch(indexNameV1, itemType, work1)

        val work2 = workWith(
          canonicalId = "5678",
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
                 |     "id": "${work2.canonicalId}",
                 |     "title": "${work2.title.get}",
                 |     "contributors": [ ],
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
