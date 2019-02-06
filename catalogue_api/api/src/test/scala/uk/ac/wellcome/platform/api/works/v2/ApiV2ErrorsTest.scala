package uk.ac.wellcome.platform.api.works.v2

import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.platform.api.works.ApiErrorsTestBase
import uk.ac.wellcome.fixtures.TestWith

class ApiV2ErrorsTest extends ApiV2WorksTestBase with ApiErrorsTestBase {
  def withServer[R](testWith: TestWith[EmbeddedHttpServer, R]): R =
    withV2Api {
      case (_, server: EmbeddedHttpServer) =>
        testWith(server)
    }

  describe("returns a 400 Bad Request for errors in the ?include parameter") {
    it("a single invalid include") {
      assertIsBadRequest(
        "/works?include=foo",
        description = "include: 'foo' is not a valid include"
      )
    }

    it("multiple invalid includes") {
      assertIsBadRequest(
        "/works?include=foo,bar",
        description = "include: 'foo', 'bar' are not valid includes"
      )
    }

    it("a mixture of valid and invalid includes") {
      assertIsBadRequest(
        "/works?include=foo,identifiers,bar",
        description = "include: 'foo', 'bar' are not valid includes"
      )
    }

    it("an invalid include on an individual work") {
      assertIsBadRequest(
        "/works/nfdn7wac?include=foo",
        description = "include: 'foo' is not a valid include"
      )
    }
  }
}
