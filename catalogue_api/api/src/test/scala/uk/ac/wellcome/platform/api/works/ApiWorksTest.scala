package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models._

class ApiWorksTest extends ApiWorksTestBase {

  it("returns a list of works") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val works = createWorks(3)

        insertIntoElasticsearch(indexName, itemType, works: _*)

        eventually {

          server.httpGet(
            path = s"/$apiPrefix/works",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              |  ${resultList(totalResults = 3)},
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
                              |     "createdDate": ${period(
                                works(0).createdDate.get)},
                              |     "creators": [ ${abstractAgent(
                                works(0).creators(0))} ],
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
                              |     "createdDate": ${period(
                                works(1).createdDate.get)},
                              |     "creators": [ ${abstractAgent(
                                works(1).creators(0))} ],
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
                              |     "createdDate": ${period(
                                works(2).createdDate.get)},
                              |     "creators": [ ${abstractAgent(
                                works(2).creators(0))} ],
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

  it("should return a single work when requested with id") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work = workWith(
          canonicalId = canonicalId,
          title = title,
          description = description,
          lettering = lettering,
          createdDate = period,
          creator = agent,
          items = List(defaultItem),
          visible = true)

        insertIntoElasticsearch(indexName, itemType, work)

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
                              | "creators": [ ${abstractAgent(work.creators(0))} ],
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

  it("should be able to render an item if the items include is present") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work = workWith(
          canonicalId = "b4heraz7",
          title = "Inside an irate igloo",
          items = List(
            itemWith(
              canonicalId = "c3a599u5",
              identifier = defaultSourceIdentifier,
              location = defaultLocation
            )
          )
        )

        insertIntoElasticsearch(indexName, itemType, work)

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
  }

  it(
    "always includes 'items' if the items include is present, even with no items") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work = workWith(
          canonicalId = "dgdb712",
          title = "Without windows or wind or washing-up liquid",
          items = List()
        )
        insertIntoElasticsearch(indexName, itemType, work)

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
                              | "creators": [ ],
                              | "items": [ ],
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

  it("includes credit information in API responses") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val location = DigitalLocation(
          locationType = "thumbnail-image",
          url = "",
          credit = Some("Wellcome Collection"),
          license = License_CCBY
        )
        val item = IdentifiedItem(
          canonicalId = "chu27a8",
          sourceIdentifier = sourceIdentifier,
          identifiers = List(),
          locations = List(location)
        )
        val workWithCopyright = IdentifiedWork(
          title = Some("A scarf on a squirrel"),
          sourceIdentifier = sourceIdentifier,
          version = 1,
          canonicalId = "yxh928a",
          items = List(item))
        insertIntoElasticsearch(indexName, itemType, workWithCopyright)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?includes=items",
            andExpect = Status.Ok,
            withJsonBody = s"""
              |{
              |  ${resultList()},
              |  "results": [
              |   {
              |     "type": "Work",
              |     "id": "${workWithCopyright.canonicalId}",
              |     "title": "${workWithCopyright.title.get}",
              |     "creators": [ ],
              |     "subjects": [ ],
              |     "genres": [ ],
              |     "publishers": [ ],
              |     "placesOfPublication": [ ],
              |     "items": [
              |       {
              |         "id": "${item.canonicalId}",
              |         "type": "${item.ontologyType}",
              |         "locations": [
              |           {
              |             "type": "${location.ontologyType}",
              |             "url": "",
              |             "locationType": "${location.locationType}",
              |             "license": ${license(location.license)},
              |             "credit": "${location.credit.get}"
              |           }
              |         ]
              |       }
              |     ]
              |   }
              |  ]
              |}""".stripMargin
          )
        }
      }
    }
  }

  it(
    "returns the requested page of results when requested with page & pageSize, alongside correct next/prev links ") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val works = createWorks(3)

        insertIntoElasticsearch(indexName, itemType, works: _*)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?page=2&pageSize=1",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              |  ${resultList(
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
                              |     "createdDate": ${period(
                                works(1).createdDate.get)},
                              |     "creators": [ ${abstractAgent(
                                works(1).creators(0))} ],
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
                              |     "createdDate": ${period(
                                works(0).createdDate.get)},
                              |     "creators": [ ${abstractAgent(
                                works(0).creators(0))} ],
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
                              |     "createdDate": ${period(
                                works(2).createdDate.get)},
                              |     "creators": [ ${abstractAgent(
                                works(2).creators(0))} ],
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
  }

  it("returns a BadRequest when malformed query parameters are presented") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=penguin",
          andExpect = Status.BadRequest,
          withJsonBody = badRequest("pageSize: 'penguin' is not a valid Integer")
        )
      }
    }
  }

  it("ignores parameters that are unused when making an api request") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        server.httpGet(
          path = s"/$apiPrefix/works?foo=bar",
          andExpect = Status.Ok,
          withJsonBody = emptyJsonResult
        )
      }
    }
  }

  it(
    "returns a not found error when requesting a single work with a non existing id") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val badId = "non-existing-id"
        server.httpGet(
          path = s"/$apiPrefix/works/$badId",
          andExpect = Status.NotFound,
          withJsonBody = notFound(s"Work not found for identifier $badId")
        )
      }
    }
  }

  it(
    "returns an HTTP Bad Request error if the user asks for a page size just over the maximum") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val pageSize = 101
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=$pageSize",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(s"pageSize: [$pageSize] is not less than or equal to 100")
        )
      }
    }
  }

  it(
    "returns an HTTP Bad Request error if the user asks for an overly large page size") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val pageSize = 100000
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=$pageSize",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(s"pageSize: [$pageSize] is not less than or equal to 100")
        )
      }
    }
  }

  it(
    "returns an HTTP Bad Request error if the user asks for zero-length pages") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val pageSize = 0
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=$pageSize",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(s"pageSize: [$pageSize] is not greater than or equal to 1")
        )
      }
    }
  }

  it(
    "returns an HTTP Bad Request error if the user asks for a negative page size") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val pageSize = -50
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=$pageSize",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(s"pageSize: [$pageSize] is not greater than or equal to 1")
        )
      }
    }
  }

  it("returns an HTTP Bad Request error if the user asks for page 0") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        server.httpGet(
          path = s"/$apiPrefix/works?page=0",
          andExpect = Status.BadRequest,
          withJsonBody = badRequest("page: [0] is not greater than or equal to 1")
        )
      }
    }
  }

  it(
    "returns an HTTP Bad Request error if the user asks for a page before 0") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        server.httpGet(
          path = s"/$apiPrefix/works?page=-50",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest("page: [-50] is not greater than or equal to 1")
        )
      }
    }
  }

  it("returns multiple errors if there's more than one invalid parameter") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=-60&page=-50",
          andExpect = Status.BadRequest,
          withJsonBody = badRequest(
            "page: [-50] is not greater than or equal to 1, pageSize: [-60] is not greater than or equal to 1")
        )
      }
    }
  }

  it("returns matching results if doing a full-text search") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work1 = workWith(
          canonicalId = "1234",
          title = "A drawing of a dodo"
        )
        val work2 = workWith(
          canonicalId = "5678",
          title = "A mezzotint of a mouse"
        )
        insertIntoElasticsearch(indexName, itemType, work1, work2)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?query=cat",
            andExpect = Status.Ok,
            withJsonBody = emptyJsonResult
          )
        }

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?query=dodo",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              |  ${resultList()},
                              |  "results": [
                              |   {
                              |     "type": "Work",
                              |     "id": "${work1.canonicalId}",
                              |     "title": "${work1.title.get}",
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
  }

  it("includes subject information in API responses") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val workWithSubjects = IdentifiedWork(
          title = Some("A seal selling seaweed sandwiches in Scotland"),
          sourceIdentifier = sourceIdentifier,
          version = 1,
          identifiers = List(),
          canonicalId = "test_subject1",
          subjects = List(Concept("fish"), Concept("gardening"))
        )
        insertIntoElasticsearch(indexName, itemType, workWithSubjects)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              |  ${resultList()},
                              |  "results": [
                              |   {
                              |     "type": "Work",
                              |     "id": "${workWithSubjects.canonicalId}",
                              |     "title": "${workWithSubjects.title.get}",
                              |     "creators": [],
                              |     "subjects": [ ${concepts(
                                workWithSubjects.subjects)} ],
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
  }

<<<<<<< HEAD
  it("includes genre information in API responses") {
    withLocalElasticsearchIndex(itemType) { indexName =>
=======
  it("should include genre information in API responses") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
>>>>>>> Give the type system some help, it's a bit dim today
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val workWithSubjects = IdentifiedWork(
          title = Some("A guppy in a greenhouse"),
          sourceIdentifier = sourceIdentifier,
          version = 1,
          identifiers = List(),
          canonicalId = "test_subject1",
          genres = List(Concept("woodwork"), Concept("etching"))
        )
        insertIntoElasticsearch(indexName, itemType, workWithSubjects)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              |  ${resultList()},
                              |  "results": [
                              |   {
                              |     "type": "Work",
                              |     "id": "${workWithSubjects.canonicalId}",
                              |     "title": "${workWithSubjects.title.get}",
                              |     "creators": [],
                              |     "subjects": [ ],
                              |     "genres": [ ${concepts(workWithSubjects.genres)} ],
                              |     "publishers": [ ],
                              |     "placesOfPublication": [ ]
                              |   }
                              |  ]
                              |}""".stripMargin
          )
        }
      }
    }
  }

  it(
    "includes a list of identifiers on a list endpoint if we pass ?includes=identifiers") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val identifier1 = SourceIdentifier(
          identifierScheme = IdentifierSchemes.miroImageNumber,
          value = "Test1234"
        )
        val work1 = workWith(
          canonicalId = "1234",
          title = "An image of an iguana",
          identifiers = List(identifier1)
        )

        val identifier2 = SourceIdentifier(
          identifierScheme = IdentifierSchemes.miroImageNumber,
          value = "DTest5678"
        )
        val work2 = workWith(
          canonicalId = "5678",
          title = "An impression of an igloo",
          identifiers = List(identifier2)
        )

        insertIntoElasticsearch(indexName, itemType, work1, work2)

        val works = List(work1, work2)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?includes=identifiers",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              |  ${resultList(totalResults = 2)},
                              |  "results": [
                              |   {
                              |     "type": "Work",
                              |     "id": "${work1.canonicalId}",
                              |     "title": "${work1.title.get}",
                              |     "creators": [ ],
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
                              |     "creators": [ ],
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
  }

  it(
    "includes a list of identifiers on a single work endpoint if we pass ?includes=identifiers") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val srcIdentifier = SourceIdentifier(
          identifierScheme = IdentifierSchemes.miroImageNumber,
          value = "Test1234"
        )
        val work = workWith(
          canonicalId = "1234",
          title = "An insect huddled in an igloo",
          identifiers = List(srcIdentifier)
        )
        insertIntoElasticsearch(indexName, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?includes=identifiers",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              | "@context": "https://localhost:8888/$apiPrefix/context.json",
                              | "type": "Work",
                              | "id": "${work.canonicalId}",
                              | "title": "${work.title.get}",
                              | "creators": [ ],
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
  }

  it(
    "includes 'identifiers' with the identifiers include, even if there are no identifiers") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work = workWith(
          canonicalId = "a87na87",
          title = "Idling inkwells of indigo images",
          identifiers = List()
        )
        insertIntoElasticsearch(indexName, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?includes=identifiers",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              | "@context": "https://localhost:8888/$apiPrefix/context.json",
                              | "type": "Work",
                              | "id": "${work.canonicalId}",
                              | "title": "${work.title.get}",
                              | "creators": [ ],
                              | "identifiers": [ ],
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

<<<<<<< HEAD
  it("looks at different indices based on the ?index query parameter") {
    withLocalElasticsearchIndex(itemType) { indexName =>
=======
  it(
    "can look at different Elasticsearch indices based on the ?index query parameter") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
>>>>>>> Give the type system some help, it's a bit dim today
      val flags = esLocalFlags(indexName, itemType)
      withLocalElasticsearchIndex(itemType = itemType) { altIndexName =>
        withServer(flags) { server =>

          val work = workWith(
            canonicalId = "1234",
            title = "A whale on a wave"
          )
          insertIntoElasticsearch(indexName, itemType, work)

          val work_alt = workWith(
            canonicalId = "5678",
            title = "An impostor in an igloo"
          )
          insertIntoElasticsearchWithIndex(altIndexName, itemType, work_alt)

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
              path = s"/$apiPrefix/works/${work_alt.canonicalId}?_index=$altIndexName",
              andExpect = Status.Ok,
              withJsonBody = s"""
                                |{
                                | "@context": "https://localhost:8888/$apiPrefix/context.json",
                                | "type": "Work",
                                | "id": "${work_alt.canonicalId}",
                                | "title": "${work_alt.title.get}",
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
  }

<<<<<<< HEAD
  it("searches different indices based on the ?_index query parameter") {
    withLocalElasticsearchIndex(itemType) { indexName =>
=======
  it(
    "can search different Elasticsearch indices based on the ?_index query parameter") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
>>>>>>> Give the type system some help, it's a bit dim today
      val flags = esLocalFlags(indexName, itemType)
      withLocalElasticsearchIndex(itemType = itemType) { altIndexName =>
        withServer(flags) { server =>
          val work = workWith(
            canonicalId = "1234",
            title = "A wombat wallowing under a willow"
          )
          insertIntoElasticsearch(indexName, itemType, work)

          val work_alt = workWith(
            canonicalId = "5678",
            title = "An impostor in an igloo"
          )
          insertIntoElasticsearchWithIndex(altIndexName, itemType, work_alt)

          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works?query=wombat",
              andExpect = Status.Ok,
              withJsonBody = s"""
                                |{
                                |  ${resultList()},
                                |  "results": [
                                |   {
                                |     "type": "Work",
                                |     "id": "${work.canonicalId}",
                                |     "title": "${work.title.get}",
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
              path = s"/$apiPrefix/works?query=igloo&_index=$altIndexName",
              andExpect = Status.Ok,
              withJsonBody = s"""
                                |{
                                |  ${resultList()},
                                |  "results": [
                                |   {
                                |     "type": "Work",
                                |     "id": "${work_alt.canonicalId}",
                                |     "title": "${work_alt.title.get}",
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
  }

  it("returns a Bad Request error if asked for an invalid include") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?includes=foo",
            andExpect = Status.BadRequest,
            withJsonBody = badRequest("includes: 'foo' is not a valid include")
          )
        }
      }
    }
  }

  it(
    "returns a Bad Request error if asked for more than one invalid include") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?includes=foo,bar",
            andExpect = Status.BadRequest,
            withJsonBody =
              badRequest("includes: 'foo', 'bar' are not valid includes")
          )
        }
      }
    }
  }

  it(
<<<<<<< HEAD
=======
    "returns a Bad Request error if asked for a mixture of valid and invalid includes") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?includes=foo,identifiers,bar",
            andExpect = Status.BadRequest,
            withJsonBody =
              badRequest("includes: 'foo', 'bar' are not valid includes")
          )
        }
      }
    }
  }

  it(
>>>>>>> Give the type system some help, it's a bit dim today
    "returns a Bad Request error if asked for an invalid include on an individual work") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/nfdn7wac?includes=foo",
            andExpect = Status.BadRequest,
            withJsonBody = badRequest("includes: 'foo' is not a valid include")
          )
        }
      }
    }
  }

<<<<<<< HEAD
=======
  it(
    "includes the thumbnail field if available and we use the thumbnail include") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work = identifiedWorkWith(
          canonicalId = "1234",
          title = "A thorn in the thumb tells a traumatic tale",
          thumbnail = DigitalLocation(
            locationType = "thumbnail-image",
            url = "https://iiif.example.org/1234/default.jpg",
            license = License_CCBY
          )
        )
        insertIntoElasticsearch(indexName, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?includes=thumbnail",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              |  ${resultList()},
                              |  "results": [
                              |   {
                              |     "type": "Work",
                              |     "id": "${work.canonicalId}",
                              |     "title": "${work.title.get}",
                              |     "creators": [ ],
                              |     "subjects": [ ],
                              |     "genres": [ ],
                              |     "publishers": [ ],
                              |     "placesOfPublication": [ ],
                              |     "thumbnail": ${location(work.thumbnail.get)}
                              |   }
                              |  ]
                              |}
              """.stripMargin
          )
        }
      }
    }
  }

  it("does not include the thumbnail if we omit the thumbnail include") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val work = identifiedWorkWith(
          canonicalId = "5678",
          title = "An otter omitted from an occasion in Oslo",
          thumbnail = DigitalLocation(
            locationType = "thumbnail-image",
            url = "",
            license = License_CCBY
          )
        )
        insertIntoElasticsearch(indexName, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              |  ${resultList()},
                              |  "results": [
                              |   {
                              |     "type": "Work",
                              |     "id": "${work.canonicalId}",
                              |     "title": "${work.title.get}",
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

>>>>>>> Give the type system some help, it's a bit dim today
  it("returns Not Found if you look up a non-existent index") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?_index=foobarbaz",
            andExpect = Status.NotFound,
            withJsonBody = notFound("There is no index foobarbaz")
          )
        }
      }
    }
  }

  it(
    "returns an Internal Server error if you try to search a malformed index") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>

        // We need to do something that reliably triggers an internal exception
        // in the Elasticsearch handler.
        //
        // Elasticsearch has a number of "private" indexes, which don't have
        // a canonicalId field to sort on.  Trying to query one of these will
        // trigger one such exception!
        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?_index=.watches",
            andExpect = Status.InternalServerError,
            withJsonBody = s"""{
              "@context": "https://localhost:8888/$apiPrefix/context.json",
              "type": "Error",
              "errorType": "http",
              "httpStatus": 500,
              "label": "Internal Server Error"
            }"""
          )
        }
      }
    }
  }

  it("returns a Bad Request error if you try to page beyond the first 10000 works") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val queries = List(
          "page=10000",
          "pageSize=100&page=101",
          "page=126&pageSize=80"
        )
        queries.foreach { query =>
          println(s"Testing query=$query")
          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works?$query",
              andExpect = Status.BadRequest,
              withJsonBody =
                badRequest("Only the first 10000 works are available in the API.")
            )
          }
        }
      }
    }
  }
}
