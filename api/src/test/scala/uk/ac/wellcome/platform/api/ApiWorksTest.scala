package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.FunSpec
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal

class ApiWorksTest
    extends FunSpec
    with FeatureTestMixin
    with IndexedElasticSearchLocal
    with WorksUtil {

  implicit val jsonMapper = Work
  override val server =
    new EmbeddedHttpServer(
      new Server,
      flags = Map(
        "es.host" -> "localhost",
        "es.port" -> 9200.toString,
        "es.name" -> "wellcome",
        "es.index" -> indexName,
        "es.type" -> itemType
      )
    )

  val apiPrefix = "catalogue/v0"

  val emptyJsonResult = s"""
                           |{
                           |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                           |  "type": "ResultList",
                           |  "pageSize": 10,
                           |  "totalPages": 0,
                           |  "totalResults": 0,
                           |  "results": []
                           |}""".stripMargin

  def items(work: Work) = {
    work.items
      .map { it =>
        s"""{
         "id": "${it.canonicalId.get}",
         "type": "${it.ontologyType}",
         "locations": [
           ${locations(it)}
         ]
       }"""
      }
      .mkString(",")
  }

  private def locations(it: Item) = {
    it.locations
      .map { location =>
        s"""{
           "type": "${location.ontologyType}",
           "locationType": "${location.locationType}",
           "url": "${location.url.get}",
           "license": ${license(location)}
           }"""

      }
      .mkString(",")
  }

  private def license(location: Location) = {
    s"""{
         "label": "${location.license.label}",
         "licenseType": "${location.license.licenseType}",
         "type": "${location.license.ontologyType}",
         "url": "${location.license.url}"
         }"""
  }

  it("should return a list of works") {

    val works = createWorks(3)

    insertIntoElasticSearch(works: _*)

    eventually {

      server.httpGet(
        path = s"/$apiPrefix/works",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 10,
                          |  "totalPages": 1,
                          |  "totalResults": 3,
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(0).id}",
                          |     "title": "${works(0).title}",
                          |     "description": "${works(0).description.get}",
                          |     "lettering": "${works(0).lettering.get}",
                          |     "createdDate": {
                          |       "type": "Period",
                          |       "label": "${works(0).createdDate.get.label}"
                          |     },
                          |     "creators": [{
                          |       "type": "Agent",
                          |       "label": "${works(0).creators(0).label}"
                          |     }],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [
                          |       ${items(works(0))}
                          |     ]
                          |   },
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(1).id}",
                          |     "title": "${works(1).title}",
                          |     "description": "${works(1).description.get}",
                          |     "lettering": "${works(1).lettering.get}",
                          |     "createdDate": {
                          |       "type": "Period",
                          |       "label": "${works(1).createdDate.get.label}"
                          |     },
                          |     "creators": [{
                          |       "type": "Agent",
                          |       "label": "${works(1).creators(0).label}"
                          |     }],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [
                          |       ${items(works(1))}
                          |     ]
                          |   },
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(2).id}",
                          |     "title": "${works(2).title}",
                          |     "description": "${works(2).description.get}",
                          |     "lettering": "${works(2).lettering.get}",
                          |     "createdDate": {
                          |       "type": "Period",
                          |       "label": "${works(2).createdDate.get.label}"
                          |     },
                          |     "creators": [{
                          |       "type": "Agent",
                          |       "label": "${works(2).creators(0).label}"
                          |     }],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [
                          |       ${items(works(2))}
                          |     ]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

  it("should return a single work when requested with id") {
    val work = workWith(
      canonicalId = canonicalId,
      title = title,
      description = description,
      lettering = lettering,
      createdDate = period,
      creator = agent
    )

    insertIntoElasticSearch(work)

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
                          | "lettering": "$lettering",
                          | "createdDate": {
                          |   "type": "Period",
                          |   "label": "${period.label}"
                          | },
                          | "creators": [{
                          |   "type": "Agent",
                          |   "label": "${agent.label}"
                          | }],
                          | "items": [${items(work)}],
                          | "subjects": [ ],
                          | "genres": [ ]
                          |}
          """.stripMargin
      )
    }
  }

  it(
    "should return the requested page of results when requested with page & pageSize, alongside correct next/prev links ") {
    val works = createWorks(3)

    insertIntoElasticSearch(works: _*)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?page=2&pageSize=1",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 1,
                          |  "totalPages": 3,
                          |  "totalResults": 3,
                          |  "prevPage": "https://localhost:8888/$apiPrefix/works?page=1&pageSize=1",
                          |  "nextPage": "https://localhost:8888/$apiPrefix/works?page=3&pageSize=1",
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(1).id}",
                          |     "title": "${works(1).title}",
                          |     "description": "${works(1).description.get}",
                          |     "lettering": "${works(1).lettering.get}",
                          |     "createdDate": {
                          |       "type": "Period",
                          |       "label": "${works(1).createdDate.get.label}"
                          |     },
                          |     "creators": [{
                          |       "type": "Agent",
                          |       "label": "${works(1).creators(0).label}"
                          |     }],
                          |     "items": [ ${items(works(1))}],
                          |     "subjects": [ ],
                          |     "genres": [ ]
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
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 1,
                          |  "totalPages": 3,
                          |  "totalResults": 3,
                          |  "nextPage": "https://localhost:8888/$apiPrefix/works?page=2&pageSize=1",
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(0).id}",
                          |     "title": "${works(0).title}",
                          |     "description": "${works(0).description.get}",
                          |     "lettering": "${works(0).lettering.get}",
                          |     "createdDate": {
                          |       "type": "Period",
                          |       "label": "${works(0).createdDate.get.label}"
                          |     },
                          |     "creators": [{
                          |       "type": "Agent",
                          |       "label": "${works(0).creators(0).label}"
                          |     }],
                          |     "items": [${items(works(0))}],
                          |     "subjects": [ ],
                          |     "genres": [ ]
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
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 1,
                          |  "totalPages": 3,
                          |  "totalResults": 3,
                          |  "prevPage": "https://localhost:8888/$apiPrefix/works?page=2&pageSize=1",
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(2).id}",
                          |     "title": "${works(2).title}",
                          |     "description": "${works(2).description.get}",
                          |     "lettering": "${works(2).lettering.get}",
                          |     "createdDate": {
                          |       "type": "Period",
                          |       "label": "${works(2).createdDate.get.label}"
                          |     },
                          |     "creators": [{
                          |       "type": "Agent",
                          |       "label": "${works(2).creators(0).label}"
                          |     }],
                          |     "subjects": [ ],
                          |      "items": [${items(works(2))}],
                          |     "genres": [ ]
                          |   }]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

  it("should return a BadRequest when malformed query parameters are presented") {
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=penguin",
      andExpect = Status.BadRequest,
      withJsonBody = """
                       |{
                       |  "errors" : [
                       |    "pageSize: 'penguin' is not a valid Integer"
                       |  ]
                       |}
                     """.stripMargin
    )
  }

  it("should ignore parameters that are unused when making an api request") {
    server.httpGet(
      path = s"/$apiPrefix/works?foo=bar",
      andExpect = Status.Ok,
      withJsonBody = emptyJsonResult
    )
  }

  it(
    "should return a not found error when requesting a single work with a non existing id") {
    server.httpGet(
      path = s"/$apiPrefix/works/non-existing-id",
      andExpect = Status.NotFound,
      withJsonBody = ""
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for a page size just over the maximum") {
    val pageSize = 101
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody =
        s"""{"errors":["pageSize: [$pageSize] is not less than or equal to 100"]}"""
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for an overly large page size") {
    val pageSize = 100000
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody =
        s"""{"errors":["pageSize: [$pageSize] is not less than or equal to 100"]}"""
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for zero-length pages") {
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=0",
      andExpect = Status.BadRequest,
      withJsonBody =
        s"""{"errors":["pageSize: [0] is not greater than or equal to 1"]}"""
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for a negative page size") {
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=-50",
      andExpect = Status.BadRequest,
      withJsonBody =
        s"""{"errors":["pageSize: [-50] is not greater than or equal to 1"]}"""
    )
  }

  it("should return an HTTP Bad Request error if the user asks for page 0") {
    server.httpGet(
      path = s"/$apiPrefix/works?page=0",
      andExpect = Status.BadRequest,
      withJsonBody =
        s"""{"errors":["page: [0] is not greater than or equal to 1"]}"""
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for a page before 0") {
    server.httpGet(
      path = s"/$apiPrefix/works?page=-50",
      andExpect = Status.BadRequest,
      withJsonBody =
        s"""{"errors":["page: [-50] is not greater than or equal to 1"]}"""
    )
  }

  it("should return multiple errors if there's more than one invalid parameter") {
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=-60&page=-50",
      andExpect = Status.BadRequest,
      withJsonBody = s"""
                        |{
                        |  "errors": [
                        |    "page: [-50] is not greater than or equal to 1",
                        |    "pageSize: [-60] is not greater than or equal to 1"
                        |  ]
                        |}
      """.stripMargin
    )
  }

  it("should return matching results if doing a full-text search") {
    val work1 = workWith(
      canonicalId = "1234",
      title = "A drawing of a dodo"
    )
    val work2 = workWith(
      canonicalId = "5678",
      title = "A mezzotint of a mouse"
    )
    insertIntoElasticSearch(work1, work2)

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
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 10,
                          |  "totalPages": 1,
                          |  "totalResults": 1,
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work1.id}",
                          |     "title": "${work1.title}",
                          |     "creators": [],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [ ]
                          |   }
                          |  ]
                          |}""".stripMargin
      )
    }
  }

  it("should include subject information in API responses") {
    val workWithSubjects = Work(
      canonicalId = Some("test_subject1"),
      identifiers = List(),
      title = "A seal selling seaweed sandwiches in Scotland",
      subjects = List(Concept("fish"), Concept("gardening"))
    )
    insertIntoElasticSearch(workWithSubjects)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 10,
                          |  "totalPages": 1,
                          |  "totalResults": 1,
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.id}",
                          |     "title": "${workWithSubjects.title}",
                          |     "creators": [],
                          |     "subjects": [
                          |      {
                          |        "type": "Concept",
                          |        "label": "fish"
                          |      },
                          |      {
                          |        "type": "Concept",
                          |        "label": "gardening"
                          |      }
                          |     ],
                          |     "genres": [ ],
                          |     "items": [ ]
                          |   }
                          |  ]
                          |}""".stripMargin
      )
    }
  }

  it("should include genre information in API responses") {
    val workWithSubjects = Work(
      canonicalId = Some("test_subject1"),
      identifiers = List(),
      title = "A guppy in a greenhouse",
      genres = List(Concept("woodwork"), Concept("etching"))
    )
    insertIntoElasticSearch(workWithSubjects)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 10,
                          |  "totalPages": 1,
                          |  "totalResults": 1,
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.id}",
                          |     "title": "${workWithSubjects.title}",
                          |     "creators": [],
                          |     "subjects": [ ],
                          |     "genres": [
                          |       {
                          |         "type": "Concept",
                          |         "label": "woodwork"
                          |       },
                          |       {
                          |         "type": "Concept",
                          |         "label": "etching"
                          |       }
                          |     ],
                          |     "items": [ ]
                          |   }
                          |  ]
                          |}""".stripMargin
      )
    }
  }

  it(
    "should include a list of identifiers on a list endpoint if we pass ?includes=identifiers") {
    val identifier1 = SourceIdentifier(
      identifierScheme = "The ID field within the TestSource",
      value = "Test1234"
    )
    val work1 = workWith(
      canonicalId = "1234",
      title = "An image of an iguana",
      identifiers = List(identifier1)
    )

    val identifier2 = SourceIdentifier(
      identifierScheme = "The ID field within the DifferentTestSource",
      value = "DTest5678"
    )
    val work2 = workWith(
      canonicalId = "5678",
      title = "An impression of an igloo",
      identifiers = List(identifier2)
    )

    insertIntoElasticSearch(work1, work2)

    val works = List(work1, work2)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=identifiers",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 10,
                          |  "totalPages": 1,
                          |  "totalResults": 2,
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work1.id}",
                          |     "title": "${work1.title}",
                          |     "creators": [ ],
                          |     "identifiers": [
                          |       {
                          |         "type": "Identifier",
                          |         "identifierScheme": "${identifier1.identifierScheme}",
                          |         "value": "${identifier1.value}"
                          |       }
                          |     ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [ ]
                          |   },
                          |   {
                          |     "type": "Work",
                          |     "id": "${work2.id}",
                          |     "title": "${work2.title}",
                          |     "creators": [ ],
                          |     "identifiers": [
                          |       {
                          |         "type": "Identifier",
                          |         "identifierScheme": "${identifier2.identifierScheme}",
                          |         "value": "${identifier2.value}"
                          |       }
                          |     ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [ ]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

  it(
    "should include a list of identifiers on a single work endpoint if we pass ?includes=identifiers") {
    val identifier = SourceIdentifier(
      identifierScheme = "An Insectoid Identifier",
      value = "Test1234"
    )
    val work = workWith(
      canonicalId = "1234",
      title = "An insect huddled in an igloo",
      identifiers = List(identifier)
    )
    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work.id}?includes=identifiers",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          | "@context": "https://localhost:8888/$apiPrefix/context.json",
                          | "type": "Work",
                          | "id": "${work.id}",
                          | "title": "${work.title}",
                          | "creators": [ ],
                          | "identifiers": [
                          |   {
                          |     "type": "Identifier",
                          |     "identifierScheme": "${identifier.identifierScheme}",
                          |     "value": "${identifier.value}"
                          |   }
                          | ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "items": [ ]
                          |}
          """.stripMargin
      )
    }
  }

  it(
    "should be able to look at different Elasticsearch indices based on the ?index query parameter") {
    val work = workWith(
      canonicalId = "1234",
      title = "A whale on a wave"
    )
    insertIntoElasticSearch(work)

    val work_alt = workWith(
      canonicalId = "5678",
      title = "An impostor in an igloo"
    )
    insertIntoElasticSearchWithIndex("alt_records", work_alt)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work.id}",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          | "@context": "https://localhost:8888/$apiPrefix/context.json",
                          | "type": "Work",
                          | "id": "${work.id}",
                          | "title": "${work.title}",
                          | "creators": [ ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "items": [ ]
                          |}
          """.stripMargin
      )
    }

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work_alt.id}?_index=alt_records",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          | "@context": "https://localhost:8888/$apiPrefix/context.json",
                          | "type": "Work",
                          | "id": "${work_alt.id}",
                          | "title": "${work_alt.title}",
                          | "creators": [ ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "items": [ ]
                          |}
          """.stripMargin
      )
    }
  }

  it(
    "should be able to search different Elasticsearch indices based on the ?index query parameter") {
    val work = workWith(
      canonicalId = "1234",
      title = "A wombat wallowing under a willow"
    )
    insertIntoElasticSearch(work)

    val work_alt = workWith(
      canonicalId = "5678",
      title = "An impostor in an igloo"
    )
    insertIntoElasticSearchWithIndex("alt_records", work_alt)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?query=wombat",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 10,
                          |  "totalPages": 1,
                          |  "totalResults": 1,
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work.id}",
                          |     "title": "${work.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [ ]
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
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 10,
                          |  "totalPages": 1,
                          |  "totalResults": 1,
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work_alt.id}",
                          |     "title": "${work_alt.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [ ]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

  it("should return a Bad Request error if asked for an invalid include") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=foo",
        andExpect = Status.BadRequest,
        withJsonBody =
          """{"errors" : ["includes: 'foo' is not a valid include"]}"""
      )
    }
  }

  it(
    "should return a Bad Request error if asked for more than one invalid include") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=foo,bar",
        andExpect = Status.BadRequest,
        withJsonBody =
          """{"errors" : ["includes: 'foo', 'bar' are not valid includes"]}"""
      )
    }
  }

  it(
    "should return a Bad Request error if asked for a mixture of valid and invalid includes") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=foo,identifiers,bar",
        andExpect = Status.BadRequest,
        withJsonBody =
          """{"errors" : ["includes: 'foo', 'bar' are not valid includes"]}"""
      )
    }
  }

  it(
    "should return a Bad Request error if asked for an invalid include on an individual work") {
    val work = workWith(
      canonicalId = "1234",
      title = "A emu and an elephant"
    )
    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work.id}?includes=foo",
        andExpect = Status.BadRequest,
        withJsonBody =
          """{"errors" : ["includes: 'foo' is not a valid include"]}"""
      )
    }
  }

  it(
    "should include the thumbnail field if available and we use the thumbnail include") {
    val work = identifiedWorkWith(
      canonicalId = "1234",
      title = "A thorn in the thumb tells a traumatic tale",
      thumbnail = Location(
        locationType = "thumbnail-image",
        url = Some("https://iiif.example.org/1234/default.jpg"),
        license = License(
          licenseType = "CC-test",
          label = "A fictional license for testing",
          url = "http://creativecommons.org/licenses/test/-1.0/"
        )
      )
    )
    insertIntoElasticSearch(work)

    val thumbnail: Location = work.thumbnail.get
    val license: BaseLicense = thumbnail.license

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=thumbnail",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 10,
                          |  "totalPages": 1,
                          |  "totalResults": 1,
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work.id}",
                          |     "title": "${work.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [ ],
                          |     "thumbnail": {
                          |       "type": "Location",
                          |       "locationType": "${thumbnail.locationType}",
                          |       "url": "${thumbnail.url.get}",
                          |       "license": {
                          |         "type": "License",
                          |         "licenseType": "${license.licenseType}",
                          |         "label": "${license.label}",
                          |         "url": "${license.url}"
                          |       }
                          |     }
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

  it("should not include the thumbnail if we omit the thumbnail include") {
    val work = identifiedWorkWith(
      canonicalId = "5678",
      title = "An otter omitted from an occasion in Oslo",
      thumbnail = Location(
        locationType = "thumbnail-image",
        license = License(
          licenseType = "CC-toast",
          label = "A fictional license for toasting",
          url = "http://creativecommons.org/licenses/toast/-slice/"
        )
      )
    )
    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                          |  "type": "ResultList",
                          |  "pageSize": 10,
                          |  "totalPages": 1,
                          |  "totalResults": 1,
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work.id}",
                          |     "title": "${work.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "items": [ ]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

}
