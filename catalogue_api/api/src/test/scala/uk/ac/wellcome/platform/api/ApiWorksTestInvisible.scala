package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.Status

class ApiWorksTestInvisible extends ApiWorksTestBase {

  it("returns an HTTP 410 Gone if looking up a work with visible = false") {
    val work = workWith(
      canonicalId = "g9dtcj2e",
      title = "This work has been deleted",
      visible = false
    )

    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work.canonicalId.get}",
        andExpect = Status.Gone,
        withJsonBody = gone(work.title)
      )
    }
  }

}
