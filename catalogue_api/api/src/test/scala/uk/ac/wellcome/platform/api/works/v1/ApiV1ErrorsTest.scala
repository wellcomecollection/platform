package uk.ac.wellcome.platform.api.works.v1

import uk.ac.wellcome.platform.api.works.ApiErrorsTestBase

class ApiV1ErrorsTest extends ApiV1WorksTestBase with ApiErrorsTestBase {
  describe("returns a 400 Bad Request for errors in the ?includes parameter") {
    it("a single invalid include") {
      assertIsBadRequest(
        "/works?includes=foo",
        description = "includes: 'foo' is not a valid include"
      )
    }

    it("multiple invalid includes") {
      assertIsBadRequest(
        "/works?includes=foo,bar",
        description = "includes: 'foo', 'bar' are not valid includes"
      )
    }

    it("a mixture of valid and invalid includes") {
      assertIsBadRequest(
        "/works?includes=foo,identifiers,bar",
        description = "includes: 'foo', 'bar' are not valid includes"
      )
    }

    it("an invalid include on an individual work") {
      assertIsBadRequest(
        "/works/nfdn7wac?includes=foo",
        description = "includes: 'foo' is not a valid include"
      )
    }
  }
}
