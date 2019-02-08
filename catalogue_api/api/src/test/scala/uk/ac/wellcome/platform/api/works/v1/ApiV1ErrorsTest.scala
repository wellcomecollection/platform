package uk.ac.wellcome.platform.api.works.v1

import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.platform.api.works.ApiErrorsTestBase
import uk.ac.wellcome.fixtures.TestWith

class ApiV1ErrorsTest extends ApiV1WorksTestBase with ApiErrorsTestBase {
  def withServer[R](testWith: TestWith[EmbeddedHttpServer, R]): R =
    withV1Api {
      case (_, server: EmbeddedHttpServer) =>
        testWith(server)
    }

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
