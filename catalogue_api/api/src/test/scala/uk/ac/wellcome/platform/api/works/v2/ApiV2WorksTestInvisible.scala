package uk.ac.wellcome.platform.api.works.v2

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork

class ApiV2WorksTestInvisible extends ApiV2WorksTestBase {
  it("returns an HTTP 410 Gone if looking up a work with visible = false") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val work = createIdentifiedInvisibleWork

        insertIntoElasticsearch(indexV2, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}",
            andExpect = Status.Gone,
            withJsonBody = gone(apiPrefix)
          )
        }
    }
  }

  it("excludes works with visible=false from list results") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val deletedWork = createIdentifiedInvisibleWork
        val works = createIdentifiedWorks(count = 2).sortBy { _.canonicalId }

        val worksToIndex = Seq[IdentifiedBaseWork](deletedWork) ++ works
        insertIntoElasticsearch(indexV2, worksToIndex: _*)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works",
            andExpect = Status.Ok,
            withJsonBody = s"""
               |{
               |  ${resultList(apiPrefix, totalResults = 2)},
               |  "results": [
               |   {
               |     "type": "Work",
               |     "id": "${works(0).canonicalId}",
               |     "title": "${works(0).title}"
               |   },
               |   {
               |     "type": "Work",
               |     "id": "${works(1).canonicalId}",
               |     "title": "${works(1).title}"
               |   }
               |  ]
               |}
          """.stripMargin
          )
        }
    }
  }

  it("excludes works with visible=false from search results") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val work = createIdentifiedWorkWith(
          title = "This shouldn't be deleted!"
        )
        val deletedWork = createIdentifiedInvisibleWork
        insertIntoElasticsearch(indexV2, work, deletedWork)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?query=deleted",
            andExpect = Status.Ok,
            withJsonBody = s"""
               |{
               |  ${resultList(apiPrefix)},
               |  "results": [
               |   {
               |     "type": "Work",
               |     "id": "${work.canonicalId}",
               |     "title": "${work.title}"
               |   }
               |  ]
               |}""".stripMargin
          )
        }
    }
  }
}
