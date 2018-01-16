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

  it("excludes works with visible=false from list results") {
    // Start by indexing a work with visible=false.
    val work = workWith(
      canonicalId = "gze7bc24",
      title = "This work has been deleted",
      visible = false
    )

    // Then we index two ordinary works into Elasticsearch.
    val works = createWorks(2)
    insertIntoElasticSearch(works: _*)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works",
        andExpect = Status.Ok,
        withJsonBody = s"""
          |{
          |  ${resultList(totalResults = 2)},
          |  "results": [
          |   {
          |     "type": "Work",
          |     "id": "${works(0).id}",
          |     "title": "${works(0).title}",
          |     "description": "${works(0).description.get}",
          |     "lettering": "${works(0).lettering.get}",
          |     "createdDate": ${period(
            works(0).createdDate.get)},
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
          |     "createdDate": ${period(
            works(1).createdDate.get)},
          |     "creators": [ ${agent(works(1).creators(0))} ],
          |     "subjects": [ ],
          |     "genres": [ ]
          |   }
          |  ]
          |}
          """.stripMargin
      )
    }
  }
}
