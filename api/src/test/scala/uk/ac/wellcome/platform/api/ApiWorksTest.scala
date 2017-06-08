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

  implicit val jsonMapper = IdentifiedWork
  override val server =
    new EmbeddedHttpServer(
      new Server,
      flags = Map(
        "es.host" -> "localhost",
        "es.port" -> 9300.toString,
        "es.name" -> "wellcome",
        "es.xpack.enabled" -> "true",
        "es.xpack.user" -> "elastic:changeme",
        "es.xpack.sslEnabled" -> "false",
        "es.sniff" -> "false"
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

  it("should return a list of works") {

    val works = createIdentifiedWorks(3)

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
            |     "id": "${works(0).canonicalId}",
            |     "label": "${works(0).work.label}",
            |     "description": "${works(0).work.description.get}",
            |     "lettering": "${works(0).work.lettering.get}",
            |     "createdDate": {
            |       "type": "Period",
            |       "label": "${works(0).work.createdDate.get.label}"
            |     },
            |     "creators": [{
            |       "type": "Agent",
            |       "label": "${works(0).work.creators(0).label}"
            |     }]
            |   },
            |   {
            |     "type": "Work",
            |     "id": "${works(1).canonicalId}",
            |     "label": "${works(1).work.label}",
            |     "description": "${works(1).work.description.get}",
            |     "lettering": "${works(1).work.lettering.get}",
            |     "createdDate": {
            |       "type": "Period",
            |       "label": "${works(1).work.createdDate.get.label}"
            |     },
            |     "creators": [{
            |       "type": "Agent",
            |       "label": "${works(1).work.creators(0).label}"
            |     }]
            |   },
            |   {
            |     "type": "Work",
            |     "id": "${works(2).canonicalId}",
            |     "label": "${works(2).work.label}",
            |     "description": "${works(2).work.description.get}",
            |     "lettering": "${works(2).work.lettering.get}",
            |     "createdDate": {
            |       "type": "Period",
            |       "label": "${works(2).work.createdDate.get.label}"
            |     },
            |     "creators": [{
            |       "type": "Agent",
            |       "label": "${works(2).work.creators(0).label}"
            |     }]
            |   }
            |  ]
            |}
          """.stripMargin
      )
    }
  }

  it("should return a single work when requested with id") {
    val identifiedWork =
      identifiedWorkWith(
        canonicalId = canonicalId,
        label = label,
        description = description,
        lettering = lettering,
        createdDate = period,
        creator = agent
      )
    insertIntoElasticSearch(identifiedWork)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/$canonicalId",
        andExpect = Status.Ok,
        withJsonBody = s"""
            |{
            | "@context": "https://localhost:8888/$apiPrefix/context.json",
            | "type": "Work",
            | "id": "$canonicalId",
            | "label": "$label",
            | "description": "$description",
            | "lettering": "$lettering",
            | "createdDate": {
            |   "type": "Period",
            |   "label": "${period.label}"
            | },
            | "creators": [{
            |   "type": "Agent",
            |   "label": "${agent.label}"
            | }]
            |}
          """.stripMargin
      )
    }
  }

  it(
    "should return the requested page of results when requested with page & pageSize") {
    val works = createIdentifiedWorks(3)

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
                          |  "results": [
                          |   {
                          |     "type": "Work",
                          |     "id": "${works(1).canonicalId}",
                          |     "label": "${works(1).work.label}",
                          |     "description": "${works(1).work.description.get}",
                          |     "lettering": "${works(1).work.lettering.get}",
                          |     "createdDate": {
                          |       "type": "Period",
                          |       "label": "${works(1).work.createdDate.get.label}"
                          |     },
                          |     "creators": [{
                          |       "type": "Agent",
                          |       "label": "${works(1).work.creators(0).label}"
                          |     }]
                          |   }]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }

  it(
    "should return a BadRequest when malformed query parameters are presented") {
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

  it("should return matching results if doing a full-text search") {
    val work1 = identifiedWorkWith(
      canonicalId = "1234",
      label = "A drawing of a dodo"
    )
    val work2 = identifiedWorkWith(
      canonicalId = "5678",
      label = "A mezzotint of a mouse"
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
             |     "id": "${work1.canonicalId}",
             |     "label": "${work1.work.label}",
             |     "creators": []
             |   }
             |  ]
             |}""".stripMargin
      )
    }
  }

  it(
    "should include a list of identifiers on a list endpoint if we pass ?includes=identifiers") {
    val identifier1 = SourceIdentifier(
      source = "TestSource",
      sourceId = "The ID field within the TestSource",
      value = "Test1234"
    )
    val work1 = identifiedWorkWith(
      canonicalId = "1234",
      label = "An image of an iguana",
      identifiers = List(identifier1)
    )

    val identifier2 = SourceIdentifier(
      source = "DifferentTestSource",
      sourceId = "The ID field within the DifferentTestSource",
      value = "DTest5678"
    )
    val work2 = identifiedWorkWith(
      canonicalId = "5678",
      label = "An impression of an igloo",
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
                          |     "id": "${work1.canonicalId}",
                          |     "label": "${work1.work.label}",
                          |     "creators": [ ],
                          |     "identifiers": [
                          |       {
                          |         "type": "Identifier",
                          |         "source": "${identifier1.source}",
                          |         "name": "${identifier1.sourceId}",
                          |         "value": "${identifier1.value}"
                          |       }
                          |     ]
                          |   },
                          |   {
                          |     "type": "Work",
                          |     "id": "${work2.canonicalId}",
                          |     "label": "${work2.work.label}",
                          |     "creators": [ ],
                          |     "identifiers": [
                          |       {
                          |         "type": "Identifier",
                          |         "source": "${identifier2.source}",
                          |         "name": "${identifier2.sourceId}",
                          |         "value": "${identifier2.value}"
                          |       }
                          |     ]
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
      source = "TestSource",
      sourceId = "The ID field within the TestSource",
      value = "Test1234"
    )
    val work = identifiedWorkWith(
      canonicalId = "1234",
      label = "An image of an iguana",
      identifiers = List(identifier)
    )
    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work.canonicalId}?includes=identifiers",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          | "@context": "https://localhost:8888/$apiPrefix/context.json",
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "label": "${work.work.label}",
                          | "creators": [ ],
                          | "identifiers": [
                          |   {
                          |     "type": "Identifier",
                          |     "source": "${identifier.source}",
                          |     "name": "${identifier.sourceId}",
                          |     "value": "${identifier.value}"
                          |   }
                          | ]
                          |}
          """.stripMargin
      )
    }
  }

  it(
    "should be able to look at different Elasticsearch indices based on the ?index query parameter") {
    val work = identifiedWorkWith(
      canonicalId = "1234",
      label = "A whale on a wave"
    )
    insertIntoElasticSearch(work)

    val work_alt = identifiedWorkWith(
      canonicalId = "5678",
      label = "An impostor in an igloo"
    )
    insertIntoElasticSearchWithIndex("alt_records", work_alt)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work.canonicalId}",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          | "@context": "https://localhost:8888/$apiPrefix/context.json",
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "label": "${work.work.label}",
                          | "creators": [ ]
                          |}
          """.stripMargin
      )
    }

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work_alt.canonicalId}?_index=alt_records",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          | "@context": "https://localhost:8888/$apiPrefix/context.json",
                          | "type": "Work",
                          | "id": "${work_alt.canonicalId}",
                          | "label": "${work_alt.work.label}",
                          | "creators": [ ]
                          |}
          """.stripMargin
      )
    }
  }

  it(
    "should be able to search different Elasticsearch indices based on the ?index query parameter") {
    val work = identifiedWorkWith(
      canonicalId = "1234",
      label = "A whale on a wave"
    )
    insertIntoElasticSearch(work)

    val work_alt = identifiedWorkWith(
      canonicalId = "5678",
      label = "An impostor in an igloo"
    )
    insertIntoElasticSearchWithIndex("alt_records", work_alt)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?query=whale",
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
                          |     "id": "${work.canonicalId}",
                          |     "label": "${work.work.label}",
                          |     "creators": [ ]
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
                          |     "id": "${work_alt.canonicalId}",
                          |     "label": "${work_alt.work.label}",
                          |     "creators": [ ]
                          |   }
                          |  ]
                          |}
          """.stripMargin
      )
    }
  }
}
