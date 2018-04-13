package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status

class ApiErrorsTest extends ApiWorksTestBase {

  it(
    "returns a BadRequest error when malformed query parameters are presented") {
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=penguin",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest("pageSize: 'penguin' is not a valid Integer")
    )
  }

  it("returns a NotFound error when requesting a work with a non-existent id") {
    val badId = "non-existing-id"
    server.httpGet(
      path = s"/$apiPrefix/works/$badId",
      andExpect = Status.NotFound,
      withJsonBody = notFound(s"Work not found for identifier $badId")
    )
  }

  it(
    "returns a BadRequest error if the user asks for a page size just over the maximum") {
    val pageSize = 101
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody =
        badRequest(s"pageSize: [$pageSize] is not less than or equal to 100")
    )
  }

  it(
    "returns a BadRequest error if the user asks for an overly large page size") {
    val pageSize = 100000
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody =
        badRequest(s"pageSize: [$pageSize] is not less than or equal to 100")
    )
  }

  it("returns a BadRequest error if the user asks for zero-length pages") {
    val pageSize = 0
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody =
        badRequest(s"pageSize: [$pageSize] is not greater than or equal to 1")
    )
  }

  it("returns a BadRequest error if the user asks for a negative page size") {
    val pageSize = -50
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=$pageSize",
      andExpect = Status.BadRequest,
      withJsonBody =
        badRequest(s"pageSize: [$pageSize] is not greater than or equal to 1")
    )
  }

  it("returns a BadRequest error if the user asks for page 0") {
    server.httpGet(
      path = s"/$apiPrefix/works?page=0",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest("page: [0] is not greater than or equal to 1")
    )
  }

  it("returns a BadRequest error if the user asks for a page before 0") {
    server.httpGet(
      path = s"/$apiPrefix/works?page=-50",
      andExpect = Status.BadRequest,
      withJsonBody =
        badRequest("page: [-50] is not greater than or equal to 1")
    )
  }

  it("returns multiple errors if there's more than one invalid parameter") {
    server.httpGet(
      path = s"/$apiPrefix/works?pageSize=-60&page=-50",
      andExpect = Status.BadRequest,
      withJsonBody = badRequest(
        "page: [-50] is not greater than or equal to 1, pageSize: [-60] is not greater than or equal to 1")
    )
  }

  it("returns a Bad Request error if asked for an invalid include") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=foo",
        andExpect = Status.BadRequest,
        withJsonBody = badRequest("includes: 'foo' is not a valid include")
      )
    }
  }

  it("returns a Bad Request error if asked for more than one invalid include") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=foo,bar",
        andExpect = Status.BadRequest,
        withJsonBody =
          badRequest("includes: 'foo', 'bar' are not valid includes")
      )
    }
  }

  it(
    "returns a Bad Request error if asked for a mixture of valid and invalid includes") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=foo,identifiers,bar",
        andExpect = Status.BadRequest,
        withJsonBody =
          badRequest("includes: 'foo', 'bar' are not valid includes")
      )
    }
  }

  it(
    "returns a Bad Request error if asked for an invalid include on an individual work") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/nfdn7wac?includes=foo",
        andExpect = Status.BadRequest,
        withJsonBody = badRequest("includes: 'foo' is not a valid include")
      )
    }
  }

  it("returns Not Found if you look up a non-existent index") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?_index=foobarbaz",
        andExpect = Status.NotFound,
        withJsonBody = notFound("There is no index foobarbaz")
      )
    }
  }

  it("returns Not Found if you ask for a non-existent work") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/xhu96f9j",
        andExpect = Status.NotFound,
        withJsonBody = notFound("Work not found for identifier xhu96f9j")
      )
    }
  }

  it("returns Bad Request if you ask for a malformed identifier") {
    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/zd224ncv]",
        andExpect = Status.BadRequest,
        withJsonBody =
          badRequest("Unrecognised character in identifier zd224ncv]")
      )
    }
  }

  it("returns an Internal Server error if you try to search a malformed index") {
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

  it(
    "returns a Bad Request error if you try to page beyond the first 10000 works") {
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
