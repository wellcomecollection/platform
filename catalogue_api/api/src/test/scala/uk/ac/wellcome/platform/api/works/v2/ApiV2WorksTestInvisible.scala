package uk.ac.wellcome.platform.api.works.v2

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.models.work.internal.IdentifiedWork

class ApiV2WorksTestInvisible extends ApiV2WorksTestBase {
  def withV2Api[R] = withApiFixtures[R](ApiVersions.v2)(_)

  it("returns an HTTP 410 Gone if looking up a work with visible = false") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val work = workWith(
          canonicalId = "g9dtcj2e",
          title = "This work has been deleted",
          visible = false
        )

        insertIntoElasticsearch(indexNameV2, itemType, work)

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
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        // Start by indexing a work with visible=false.
        val deletedWork = workWith(
          canonicalId = "gze7bc24",
          title = "This work has been deleted",
          visible = false
        )

        // Then we index two ordinary works into Elasticsearch.
        val works = createWorks(2)

        val worksToIndex = Seq[IdentifiedWork](deletedWork) ++ works
        insertIntoElasticsearch(indexNameV2, itemType, worksToIndex: _*)

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
               |     "title": "${works(0).title.get}",
               |     "description": "${works(0).description.get}",
               |     "workType": {
               |       "id": "${works(0).workType.get.id}",
               |       "label": "${works(0).workType.get.label}",
               |       "type": "WorkType"
               |     },
               |     "lettering": "${works(0).lettering.get}",
               |     "createdDate": ${period(works(0).createdDate.get)},
               |     "contributors": [${contributor(works(0).contributors(0))}],
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
               |     "createdDate": ${period(works(1).createdDate.get)},
               |     "contributors": [${contributor(works(1).contributors(0))}],
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

  it("excludes works with visible=false from search results") {
    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        val work = workWith(
          canonicalId = "r8dx6std",
          title = "A deleted dodo"
        )
        val deletedWork = workWith(
          canonicalId = "e7rxkty8",
          title = "This work has been deleted",
          visible = false
        )
        insertIntoElasticsearch(indexNameV2, itemType, work, deletedWork)

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
               |     "title": "${work.title.get}",
               |     "contributors": [],
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
