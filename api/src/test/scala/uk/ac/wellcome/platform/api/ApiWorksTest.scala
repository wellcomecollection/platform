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

  private val emptyJsonResult = s"""
                           |{
                           |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                           |  "type": "ResultList",
                           |  "pageSize": 10,
                           |  "totalPages": 0,
                           |  "totalResults": 0,
                           |  "results": []
                           |}""".stripMargin

  private def items(its: List[Item]) =
    its
      .map { it =>
        s"""{
          "id": "${it.canonicalId.get}",
          "type": "${it.ontologyType}",
          "locations": [
            ${locations(it.locations)}
          ]
        }"""
      }
      .mkString(",")

  private def locations(locations: List[Location]) =
    locations
      .map { location(_) }
      .mkString(",")

  private def location(loc: Location) =
    s"""{
      "type": "${loc.ontologyType}",
      "locationType": "${loc.locationType}",
      "url": "${loc.url.get}",
      "license": ${license(loc.license)}
    }"""

  private def license(license: License) =
    s"""{
      "label": "${license.label}",
      "licenseType": "${license.licenseType}",
      "type": "${license.ontologyType}",
      "url": "${license.url}"
    }"""

  private def identifier(identifier: SourceIdentifier) =
    s"""{
      "type": "Identifier",
      "identifierScheme": "${identifier.identifierScheme}",
      "value": "${identifier.value}"
    }"""

  private def agent(ag: Agent) =
    s"""{
      "type": "Agent",
      "label": "${ag.label}"
    }"""

  private def period(p: Period) =
    s"""{
      "type": "Period",
      "label": "${p.label}"
    }"""

  private def badRequest(description: String) =
    s"""{
      "@context": "https://localhost:8888/catalogue/v0/context.json",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 400,
      "label": "Bad Request",
      "description": "$description"
    }"""

  private def concept(con: Concept) =
    s"""
    {
      "type": "${con.ontologyType}",
      "label": "${con.label}"
    }
    """

  private def concepts(concepts: List[Concept]) =
    concepts
      .map { concept(_) }
      .mkString(",")

  private def resultList(pageSize: Int = 10, totalPages: Int = 1, totalResults: Int = 1) =
    s"""
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "ResultList",
      "pageSize": $pageSize,
      "totalPages": $totalPages,
      "totalResults": $totalResults
    """

  private def notFound(description: String) =
    s"""{
      "@context": "https://localhost:8888/catalogue/v0/context.json",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 404,
      "label": "Not Found",
      "description": "$description"
    }"""

  it("should return a list of works") {

    val works = createWorks(3)

    insertIntoElasticSearch(works: _*)

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
                          |     "id": "${works(0).id}",
                          |     "title": "${works(0).title}",
                          |     "description": "${works(0).description.get}",
                          |     "lettering": "${works(0).lettering.get}",
                          |     "createdDate": ${period(works(0).createdDate.get)},
                          |     "creators": [ ${agent(works(0).creators(0))} ],
                          |     "subjects": [ ],
                          |     "genres": [ ]
                          |   },
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(1).id}",
                          |     "title": "${works(1).title}",
                          |     "description": "${works(1).description.get}",
                          |     "lettering": "${works(1).lettering.get}",
                          |     "createdDate": ${period(works(1).createdDate.get)},
                          |     "creators": [ ${agent(works(1).creators(0))} ],
                          |     "subjects": [ ],
                          |     "genres": [ ]
                          |   },
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(2).id}",
                          |     "title": "${works(2).title}",
                          |     "description": "${works(2).description.get}",
                          |     "lettering": "${works(2).lettering.get}",
                          |     "createdDate": ${period(works(2).createdDate.get)},
                          |     "creators": [ ${agent(works(2).creators(0))} ],
                          |     "subjects": [ ],
                          |     "genres": [ ]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

  it("should return a single work when requested with id") {
    val work = workWith(canonicalId = canonicalId,
                        title = title,
                        description = description,
                        lettering = lettering,
                        createdDate = period,
                        creator = agent,
                        List(defaultItem))

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
                          | "createdDate": ${period(work.createdDate.get)},
                          | "creators": [ ${agent(work.creators(0))} ],
                          | "subjects": [ ],
                          | "genres": [ ]
                          |}
          """.stripMargin
      )
    }
  }

  it("should be able to render an item if the items include is present") {
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

    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work.canonicalId.get}?includes=items",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          | "@context": "https://localhost:8888/$apiPrefix/context.json",
                          | "type": "Work",
                          | "id": "${work.canonicalId.get}",
                          | "title": "${work.title}",
                          | "creators": [ ],
                          | "items": [ ${items(work.items)} ],
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
                          |  ${resultList(pageSize = 1, totalPages = 3, totalResults = 3)},
                          |  "prevPage": "https://localhost:8888/$apiPrefix/works?page=1&pageSize=1",
                          |  "nextPage": "https://localhost:8888/$apiPrefix/works?page=3&pageSize=1",
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(1).id}",
                          |     "title": "${works(1).title}",
                          |     "description": "${works(1).description.get}",
                          |     "lettering": "${works(1).lettering.get}",
                          |     "createdDate": ${period(works(1).createdDate.get)},
                          |     "creators": [ ${agent(works(1).creators(0))} ],
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
                          |  ${resultList(pageSize = 1, totalPages = 3, totalResults = 3)},
                          |  "nextPage": "https://localhost:8888/$apiPrefix/works?page=2&pageSize=1",
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(0).id}",
                          |     "title": "${works(0).title}",
                          |     "description": "${works(0).description.get}",
                          |     "lettering": "${works(0).lettering.get}",
                          |     "createdDate": ${period(works(0).createdDate.get)},
                          |     "creators": [ ${agent(works(0).creators(0))} ],
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
                          |  ${resultList(pageSize = 1, totalPages = 3, totalResults = 3)},
                          |  "prevPage": "https://localhost:8888/$apiPrefix/works?page=2&pageSize=1",
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(2).id}",
                          |     "title": "${works(2).title}",
                          |     "description": "${works(2).description.get}",
                          |     "lettering": "${works(2).lettering.get}",
                          |     "createdDate": ${period(works(2).createdDate.get)},
                          |     "creators": [ ${agent(works(2).creators(0))} ],
                          |     "subjects": [ ],
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
      withJsonBody = badRequest("pageSize: 'penguin' is not a valid Integer")
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
    val badId = "non-existing-id"
    server.httpGet(
      path = s"/$apiPrefix/works/$badId",
      andExpect = Status.NotFound,
      withJsonBody = notFound(s"Work not found for identifier $badId")
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for a page size just over the maximum") {
    val pageSize = 101
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest(s"pageSize: [$pageSize] is not less than or equal to 100")
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for an overly large page size") {
    val pageSize = 100000
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest(s"pageSize: [$pageSize] is not less than or equal to 100")
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for zero-length pages") {
    val pageSize = 0
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest(s"pageSize: [$pageSize] is not greater than or equal to 1")
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for a negative page size") {
    val pageSize = -50
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest(s"pageSize: [$pageSize] is not greater than or equal to 1")
    )
  }

  it("should return an HTTP Bad Request error if the user asks for page 0") {
    server.httpGet(
      path = s"/$apiPrefix/works?page=0",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest("page: [0] is not greater than or equal to 1")
    )
  }

  it(
    "should return an HTTP Bad Request error if the user asks for a page before 0") {
    server.httpGet(
      path = s"/$apiPrefix/works?page=-50",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest("page: [-50] is not greater than or equal to 1")
    )
  }

  it("should return multiple errors if there's more than one invalid parameter") {
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=-60&page=-50",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest("page: [-50] is not greater than or equal to 1, pageSize: [-60] is not greater than or equal to 1")
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
                          |  ${resultList()},
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work1.id}",
                          |     "title": "${work1.title}",
                          |     "creators": [],
                          |     "subjects": [ ],
                          |     "genres": [ ]
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
                          |  ${resultList()},
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.id}",
                          |     "title": "${workWithSubjects.title}",
                          |     "creators": [],
                          |     "subjects": [ ${concepts(workWithSubjects.subjects) } ],
                          |     "genres": [ ]
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
                          |  ${resultList()},
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.id}",
                          |     "title": "${workWithSubjects.title}",
                          |     "creators": [],
                          |     "subjects": [ ],
                          |     "genres": [ ${concepts(workWithSubjects.genres)} ]
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
                          |  ${resultList(totalResults = 2)},
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work1.id}",
                          |     "title": "${work1.title}",
                          |     "creators": [ ],
                          |     "identifiers": [ ${identifier(identifier1)} ],
                          |     "subjects": [ ],
                          |     "genres": [ ]
                          |   },
                          |   {
                          |     "type": "Work",
                          |     "id": "${work2.id}",
                          |     "title": "${work2.title}",
                          |     "creators": [ ],
                          |     "identifiers": [ ${identifier(identifier2)} ],
                          |     "subjects": [ ],
                          |     "genres": [ ]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

  it(
    "should include a list of identifiers on a single work endpoint if we pass ?includes=identifiers") {
    val srcIdentifier = SourceIdentifier(
      identifierScheme = "An Insectoid Identifier",
      value = "Test1234"
    )
    val work = workWith(
      canonicalId = "1234",
      title = "An insect huddled in an igloo",
      identifiers = List(srcIdentifier)
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
                          | "identifiers": [ ${identifier(srcIdentifier)} ],
                          | "subjects": [ ],
                          | "genres": [ ]
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
                          | "genres": [ ]
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
                          | "genres": [ ]
                          |}
          """.stripMargin
      )
    }
  }

  it(
    "should be able to search different Elasticsearch indices based on the ?_index query parameter") {
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
                          |  ${resultList()},
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work.id}",
                          |     "title": "${work.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ]
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
                          |  ${resultList()},
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${work_alt.id}",
                          |     "title": "${work_alt.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ]
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
        withJsonBody = badRequest("includes: 'foo' is not a valid include")
      )
    }
  }

  it(
    "should return a Bad Request error if asked for more than one invalid include") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=foo,bar",
        andExpect = Status.BadRequest,
        withJsonBody = badRequest("includes: 'foo', 'bar' are not valid includes")
      )
    }
  }

  it(
    "should return a Bad Request error if asked for a mixture of valid and invalid includes") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=foo,identifiers,bar",
        andExpect = Status.BadRequest,
        withJsonBody = badRequest("includes: 'foo', 'bar' are not valid includes")
      )
    }
  }

  it(
    "should return a Bad Request error if asked for an invalid include on an individual work") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/nfdn7wac?includes=foo",
        andExpect = Status.BadRequest,
        withJsonBody = badRequest("includes: 'foo' is not a valid include")
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
        license = License_CCBY
      )
    )
    insertIntoElasticSearch(work)

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
                          |     "id": "${work.id}",
                          |     "title": "${work.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "thumbnail": ${location(work.thumbnail.get)}
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
        license = License_CCBY
      )
    )
    insertIntoElasticSearch(work)

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
                          |     "id": "${work.id}",
                          |     "title": "${work.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

  it("should return Not Found if you look up a non-existent index") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?_index=foobarbaz",
        andExpect = Status.NotFound,
        withJsonBody = notFound("There is no index foobarbaz")
      )
    }
  }

  it("should return an Internal Server error if you try to search a malformed index") {
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
          "@context": "https://localhost:8888/catalogue/v0/context.json",
          "type": "Error",
          "errorType": "http",
          "httpStatus": 500,
          "label": "Internal Server Error"
        }"""
      )
    }
  }

  it("should return a Bad Request error if you try to page beyond the first 10000 items") {
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
          withJsonBody = badRequest("Only the first 10000 works are available in the API.")
        )
      }
    }
  }
}
